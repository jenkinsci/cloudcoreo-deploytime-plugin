package com.cloudcoreo.plugins.jenkins;

import com.google.gson.JsonSyntaxException;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.ws.rs.ProcessingException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.String.format;


public final class CloudCoreoBuildWrapper extends SimpleBuildWrapper implements Serializable {

    private final static Logger log = Logger.getLogger(CloudCoreoBuildWrapper.class.getName());

    private static final long serialVersionUID = -2815371958535022082L;

    private CloudCoreoTeam team;

    private String teamName;
    private String customContext;

    @DataBoundConstructor
    public CloudCoreoBuildWrapper(String teamName, String context) {
        customContext = context;
        this.teamName = teamName;
    }
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @SuppressWarnings("unused")
    public String getTeamName() {
        return teamName;
    }

    @SuppressWarnings("unused")
    public String getContext() {
        return customContext;
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
                      EnvVars initialEnvironment) throws IOException {

        PrintStream logger = listener.getLogger();
        team = getDescriptor().getTeam(teamName);
        team.reloadDeployTime();

        String jobName = initialEnvironment.get("JOB_NAME");
        String ccContext = (customContext.length() > 0) ? customContext : jobName;

        log.info("Beginning setup for CloudCoreo plugin");

        String task = initialEnvironment.get("BUILD_ID");
        context.setDisposer(new ContextDisposer(context, ccContext, task, team));

        try {
            team.getDeployTime().setDeployTimeId(ccContext, task);
            team.getDeployTime().sendStartContext();

            context.env("AWS_EXECUTION_ENV", getAWSExecutionEnvironment());

            log.info("AWS_EXECUTION_ENV has been set");

            Map<String, String> vars = new HashMap<>();
            vars.put("ccTeam", team.toString());
            vars.put("ccTask", task);
            vars.put("ccContext", ccContext);
            vars.put("deployTimeID", team.getDeployTime().getDeployTimeInstance().getDeployTimeId());

            ContextDisposer.writeSerializedDataToTempFile(workspace, vars, build.getId());
            team.makeAvailable();
        } catch(ProcessingException e) {
            String message = "\nCloudCoreo server endpoint is currently unavailable, skipping DeployTime analysis\n";
            logger.println(message);
            team.makeUnavailable();
        } catch(IOException | URISyntaxException e) {
            String message = "\nError when serializing and writing data to temporary file, skipping DeployTime analysis\n";
            logger.println(message);
            team.makeUnavailable();
        } catch(JsonSyntaxException | AccessControlException | IllegalStateException e) {
            String message = "\nError retrieving DeployTime ID\n" +
                    ">> Are your CloudCoreo team ID, access key, and secret access key values correct?\n";
            logger.println(message);
            team.makeUnavailable();
        }
    }

    private String getAWSExecutionEnvironment() {
        return "CLOUDCOREO_DEVTIME CLOUDCOREO_DEVTIME_ID/" + team.getDeployTime().getDeployTimeInstance().getDeployTimeId();
    }

    private static class ContextDisposer extends Disposer {

        private final static Logger log = Logger.getLogger(ContextDisposer.class.getName());
        private static final long serialVersionUID = 2636645815974839783L;
        private final String disposerContext;
        private final String taskId;
        private final CloudCoreoTeam team;

        @SuppressWarnings("unused")
        private transient final Context context;


        ContextDisposer(Context context, String disposerContext, String taskId, CloudCoreoTeam team) {
            this.disposerContext = disposerContext;
            this.taskId = taskId;
            this.team = team;
            this.context = context;
        }

        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
            log.info("finishing CloudCoreo analysis");

            Map<String, String> vars = new HashMap<>();
            vars.put("ccTeam", team.toString());
            vars.put("ccTask", taskId);
            vars.put("ccContext", disposerContext);

            try {
                team.getDeployTime().sendStopContext();
                writeSerializedDataToTempFile(workspace, vars, build.getId());
            } catch (URISyntaxException | IOException | NullPointerException e) {
                String message = "\nThere was a problem in the teardown of the build, skipping DeployTime analysis\n";
                listener.getLogger().println(message);
                team.makeUnavailable();
            }
        }

        private static void writeSerializedDataToTempFile(FilePath path, Map<String, String> vars, String buildId)
                throws URISyntaxException, IOException {
            //create a temp file
            String fp = "file:///" + path + "/" + buildId + ".ser";
            File file = new File(new URI(fp.replaceAll(" ", "%20")).getPath());
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream s = new ObjectOutputStream(f);
            s.writeObject(vars);
            s.close();
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @SuppressWarnings("unused")
        public FormValidation doCheckPort(@QueryParameter String value) {
            if (value.equals("pallen")) return FormValidation.ok();
            else return FormValidation.error("There's a problem here");
        }

        @SuppressWarnings("unused")
        public Map<String, CloudCoreoTeam> getTeams() {
            return teams;
        }

        CloudCoreoTeam getTeam(String teamName) {
            return teams.get(teamName);
        }

        @CopyOnWrite
        private volatile Map<String, CloudCoreoTeam> teams = new LinkedHashMap<>();


        @DataBoundConstructor
        public DescriptorImpl() {
            super(CloudCoreoBuildWrapper.class);
            load();
            log.info("CloudCoreo loaded");
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            JSONArray jsonTeams;
            if (json.get("team") instanceof JSONArray) {
                jsonTeams = json.getJSONArray("team");
            } else {
                jsonTeams = new JSONArray();
                if (json.optJSONObject("team") != null) {
                    jsonTeams.add(json.getJSONObject("team"));
                }
            }
            teams = new LinkedHashMap<>(jsonTeams.size());

            try {
                for (int i = 0; i < jsonTeams.size(); i++) {
                    JSONObject instance = jsonTeams.getJSONObject(i);
                    if(!instance.containsValue("")) {
                        teams.put(instance.getString("teamName"), new CloudCoreoTeam(instance));
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warning(format("Unable to deserialize CloudCoreo teams fom JSON. %s: %s", e.getClass().getSimpleName(), e.getMessage()));
            }
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return "CloudCoreo Enabled for Workload Analysis";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
    }
}