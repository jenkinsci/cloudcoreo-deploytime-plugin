package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.ExecutionFailedException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.test.TestResultAggregator;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by paul.allen on 8/23/17.
 */
public class CloudCoreoResultArchiver extends Notifier implements MatrixAggregatable, SimpleBuildStep {

    private final static Logger log = Logger.getLogger(CloudCoreoResultArchiver.class.getName());

    private boolean blockOnHigh;
    private boolean blockOnMedium;
    private boolean blockOnLow;
    private PrintStream logger;

    private CloudCoreoTeam team;

    @SuppressWarnings({"unused", "WeakerAccess"})
    public boolean getBlockOnHigh() {
        return blockOnHigh;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public boolean getBlockOnMedium() {
        return blockOnMedium;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public boolean getBlockOnLow() {
        return blockOnLow;
    }

    CloudCoreoTeam getTeam() {
        return team;
    }

    @SuppressWarnings("WeakerAccess")
    @DataBoundConstructor
    public CloudCoreoResultArchiver(boolean blockOnHigh, boolean blockOnMedium, boolean blockOnLow) {
        this.blockOnHigh = blockOnHigh;
        this.blockOnMedium = blockOnMedium;
        this.blockOnLow = blockOnLow;
    }

    @Override
    public MatrixAggregator createAggregator(MatrixBuild matrixBuild, Launcher launcher, BuildListener buildListener) {
        return new TestResultAggregator(matrixBuild, launcher, buildListener);
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new CloudCoreoProjectAction(project);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Analyze CloudCoreo Results";
        }

        public boolean isApplicable(Class jobType) {
            return true;
        }

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    private Map<String, String> readSerializedDataFromTempFile(FilePath path, String buildId)
            throws URISyntaxException, IOException, ClassNotFoundException {
        String fp = "file:///" + path + "/" + buildId + ".ser";
        File file = new File(new URI(fp.replaceAll(" ", "%20")).getPath());
        FileInputStream f = new FileInputStream(file);
        ObjectInputStream objectStream = new ObjectInputStream(f);

        @SuppressWarnings("unchecked")
        Map<String, String> vars = (Map<String, String>) objectStream.readObject();

        objectStream.close();
        return vars;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {

        logger = listener.getLogger();
        Map<String, String> vars;
        ResultManager resultManager = new ResultManager(blockOnLow, blockOnMedium, blockOnHigh, logger);

        try {
            vars = readSerializedDataFromTempFile(workspace, build.getId());
            String teamString = vars.get("ccTeam");

            team = CloudCoreoTeam.getTeamFromString(teamString);
        } catch(URISyntaxException | IOException | ClassNotFoundException ignore) {
            build.setResult(Result.FAILURE);
            String message = "\nERROR: Could not load necessary variables, likely because of a bad serialized file.\n" +
                    ">> Are you sure you enabled CloudCoreo build environment for workload analysis?\n";
            logger.println(message);
            return;
        }

        if (!getTeam().isAvailable()) {
            String message = "\n>> Skipping CloudCoreo DeployTime analysis because access to server is unavailable.\n";
            outputMessage(message);
            return;
        }

        try {
            getTeam().getDeployTime().sendStopContext();
        } catch (Exception ex) {
            log.info("sent a stop to a torn down container");
        }

        if (build.getResult() == Result.FAILURE) {
            return;
        }
        if (!resultManager.shouldBlockBuild()) {
            // no blocking for failures requested
            return;
        }

        try {
            waitForContextRun(build);
        } catch (NullPointerException e) {
            String message = "\n>> Lost connection to CloudCoreo server, skipping DeployTime analysis.\n";
            outputMessage(message);
            return;
        } catch (ExecutionFailedException e) {
            outputMessage(e.getMessage());
            return;
        }

        try {
            resultManager.setResults(getTeam());
        } catch (Exception e) {
            String message = "\n>> There was a problem getting results, please contact us and share the following info:";
            outputMessage(message);
            outputMessage(e.getMessage());
            outputMessage(Arrays.toString(e.getStackTrace()) + "\n");
            return;
        }

        if (resultManager.hasBlockingFailures()) {
            try {
                resultManager.writeResultsToFile(workspace, build.getId());
            } catch (IOException e) {
                String message = "\n>> Error writing results to file, results will not be available for graph\n";
                outputMessage(message);
                outputMessage(e.getMessage());
            }
            resultManager.reportResultsToConsole();
            build.setResult(Result.FAILURE);
        }
    }

    private void waitForContextRun(Run<?, ?> build) throws ExecutionFailedException {
        String msg;
        boolean runHasTimedOut;
        boolean hasRunningJobs;
        do {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                msg = "Build aborted";
                build.setResult(Result.FAILURE);
                outputMessage(msg);
                return;
            }

            runHasTimedOut = getTeam().getDeployTime().contextRunTimedOut();
            hasRunningJobs = getTeam().getDeployTime().hasRunningJobs();

            if (hasRunningJobs) {
                msg = "Please wait... The job is currently executing against your cloud objects...";
            } else {
                msg = "Please wait... CloudCoreo is initializing the analysis job...";
            }
            outputMessage(msg);
        } while (!runHasTimedOut || hasRunningJobs);
        msg = "Finalizing the report...";
        outputMessage(msg);
    }

    private void outputMessage(String message) {
        logger.println(message);
        logger.flush();
        log.info(message);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
