package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.String.format;


public class CloudCoreoBuildWrapper extends SimpleBuildWrapper implements Serializable {

    private final static Logger log = Logger.getLogger(CloudCoreoBuildWrapper.class.getName());

    private static final long serialVersionUID = -2815371958535022082L;

    private CloudCoreoTeam team;
    private PrintStream logger;
    private String teamName;
    private String customContext;
    private String contextName;
    private String taskName;
    private CloudCoreoDisposer disposer;

    @SuppressWarnings("WeakerAccess")
    @DataBoundConstructor
    public CloudCoreoBuildWrapper(String teamName, String context) {
        customContext = context;
        this.teamName = teamName;
        disposer = null;
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
                      EnvVars initialEnvironment) {

        initializeVariables(listener, initialEnvironment);

        disposer = new CloudCoreoDisposer(context, contextName, taskName, team);
        context.setDisposer(getDisposer());

        try {
            team.getDeployTime().setDeployTimeId(contextName, taskName);
            team.getDeployTime().sendStartContext();

            context.env("AWS_EXECUTION_ENV", getAWSExecutionEnvironment());
            log.info("AWS_EXECUTION_ENV has been set");

            getDisposer().writeSerializedDataToTempFile(team.toString(), build);
            team.makeAvailable();
        } catch(EndpointUnavailableException e) {
            String message = e.getMessage();
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

    private void initializeVariables(TaskListener listener, EnvVars initialEnvironment) {
        logger = listener.getLogger();
        team = getTeam();
        team.loadNewDeployTime();

        String jobName = initialEnvironment.get("JOB_NAME");
        contextName = (customContext.length() > 0) ? customContext : jobName;

        log.info("Beginning setup for CloudCoreo plugin");

        taskName = initialEnvironment.get("BUILD_ID");
    }

    CloudCoreoTeam getTeam() {
        return getDescriptor().getTeam(teamName);
    }

    CloudCoreoDisposer getDisposer() {
        return disposer;
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @SuppressWarnings("unused")
        public FormValidation doCheckPort(@QueryParameter String value) {
            if (value.equals("pallen")) return FormValidation.ok();
            else return FormValidation.error("There's a problem here");
        }

        @SuppressWarnings({"unused", "WeakerAccess"})
        public Map<String, CloudCoreoTeam> getTeams() {
            return teams;
        }

        CloudCoreoTeam getTeam(String teamName) {
            return teams.get(teamName);
        }

        @CopyOnWrite
        private volatile Map<String, CloudCoreoTeam> teams = new LinkedHashMap<>();


        @SuppressWarnings("WeakerAccess")
        @DataBoundConstructor
        public DescriptorImpl() {
            super(CloudCoreoBuildWrapper.class);
            load();
            log.info("CloudCoreo loaded");
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            collectTeams(json);
            save();
            return true;
        }

        void collectTeams(JSONObject json) {
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