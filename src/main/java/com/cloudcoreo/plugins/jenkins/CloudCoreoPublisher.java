package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.ExecutionFailedException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by paul.allen on 8/23/17.
 */
public class CloudCoreoPublisher extends Notifier implements SimpleBuildStep {

    private final static Logger log = Logger.getLogger(CloudCoreoPublisher.class.getName());

    private boolean blockOnHigh;
    private boolean blockOnMedium;
    private boolean blockOnLow;
    private PrintStream logger;
    private FilePath buildPath;
    private CloudCoreoTeam team;
    private ResultManager resultManager;

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

    ResultManager getResultManager() {
        if (resultManager == null) {
            resultManager = new ResultManager(blockOnLow, blockOnMedium, blockOnHigh, logger);
        }
        return resultManager;
    }

    PrintStream getLogger() {
        return logger;
    }

    FilePath getResultsPath() {
        return buildPath;
    }

    @SuppressWarnings("WeakerAccess")
    @DataBoundConstructor
    public CloudCoreoPublisher(boolean blockOnHigh, boolean blockOnMedium, boolean blockOnLow) {
        this.blockOnHigh = blockOnHigh;
        this.blockOnMedium = blockOnMedium;
        this.blockOnLow = blockOnLow;
        buildPath = null;
        resultManager = null;
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

    String readSerializedTeamFromTempFile(String buildId)
            throws URISyntaxException, IOException, ClassNotFoundException {
        String fp = buildPath + "/" + buildId + "/team.ser";
        Path filePath = Paths.get(fp.replaceAll(" ", "%20"));

        // This should be a single line, but we'll collect as a list and join as string with no delimiter
        List<String> teamData = Files.readAllLines(filePath);
        Files.delete(filePath);
        return String.join("", teamData);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        logger = listener.getLogger();
        buildPath = new FilePath(build.getParent().getBuildDir());
        if (!getResultManager().shouldBlockBuild()) {
            // no blocking for failures requested
            return;
        }

        try {
            initializeTeam(build);
            waitForContextRun(build);
            retrieveAndSetResults(build.getId());

            writeResults(build);
            if (getResultManager().hasBlockingFailures()) {
                getResultManager().reportResultsToConsole();
                build.setResult(Result.FAILURE);
            }
        } catch (ExecutionFailedException ignore) {}
    }

    void initializeTeam(Run<?, ?> build) throws ExecutionFailedException {
        try {
            team = CloudCoreoTeam.getTeamFromString(readSerializedTeamFromTempFile(build.getId()));
        } catch(URISyntaxException | IOException | ClassNotFoundException ignore) {
            build.setResult(Result.FAILURE);
            String message = "\nERROR: Could not load necessary variables, likely because of a bad serialized file.\n" +
                    ">> Are you sure you enabled CloudCoreo build environment for workload analysis?\n";
            getLogger().println(message);
            throw new ExecutionFailedException(null);
        }

        if (!team.isAvailable()) {
            String message = "\n>> Skipping CloudCoreo DeployTime analysis because access to server is unavailable.\n";
            outputMessage(message);
            throw new ExecutionFailedException(null);
        }
    }

    private void waitForContextRun(Run<?, ?> build) throws ExecutionFailedException {
        String msg;
        try {
            pollForContextRun();
        } catch (InterruptedException e) {
            msg = "Build aborted";
            build.setResult(Result.FAILURE);
            outputMessage(msg);
            throw new ExecutionFailedException(null);
        } catch (NullPointerException e) {
            msg = "\n>> Lost connection to CloudCoreo server, skipping DeployTime analysis.\n";
            outputMessage(msg);
            throw new ExecutionFailedException(null);
        } catch (ExecutionFailedException e) {
            outputMessage(e.getMessage());
            throw new ExecutionFailedException(null);
        }
    }

    private void pollForContextRun() throws InterruptedException, ExecutionFailedException {
        boolean runHasTimedOut;
        boolean hasRunningJobs;
        String msg;
        do {
            Thread.sleep(5000);
            boolean contextRunStarted = getTeam().getDeployTime().contextRunStarted();
            runHasTimedOut = getTeam().getDeployTime().contextRunTimedOut();
            hasRunningJobs = getTeam().getDeployTime().hasRunningJobs();

            if (hasRunningJobs || contextRunStarted) {
                msg = "Please wait... The job is currently executing against your cloud objects...";
            } else {
                msg = "Please wait... CloudCoreo is initializing the analysis job...";
            }
            outputMessage(msg);
        } while (!runHasTimedOut || hasRunningJobs);
        msg = "Finalizing the report...";
        outputMessage(msg);
        // TODO: Deal with race condition at this stage where results may not be immediately ready on completion
        Thread.sleep(10000);
    }

    void retrieveAndSetResults(String buildId) {
        try {
            getResultManager().setResults(getTeam(), buildId);
        } catch (Exception e) {
            String message = "\n>> There was a problem getting results, please contact us and share the following info:";
            outputMessage(message);
            outputMessage(e.getMessage());
            outputMessage(Arrays.toString(e.getStackTrace()) + "\n");
        }
    }

    void writeResults(Run<?, ?> build) {
        try {
            getResultManager().writeResultsToFile(getResultsPath(), build.getId());
            JSONObject lastResult = ResultManager.getLastResult(getResultsPath());
            build.addAction(new CloudCoreoBuildAction(build, lastResult));
        } catch (IOException e) {
            String message = "\n>> Error writing results to file, results will not be available for graph\n";
            outputMessage(message);
            outputMessage(e.getMessage());
        }
    }

    private void outputMessage(String message) {
        getLogger().println(message);
        getLogger().flush();
        log.info(message);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
