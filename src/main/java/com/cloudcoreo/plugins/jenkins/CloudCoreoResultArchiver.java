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
public class CloudCoreoResultArchiver extends Notifier implements SimpleBuildStep {

    private final static Logger log = Logger.getLogger(CloudCoreoResultArchiver.class.getName());

    private boolean blockOnHigh;
    private boolean blockOnMedium;
    private boolean blockOnLow;
    private List<ContextTestResult> runResults;
    private String resultsHtml;

    private CloudCoreoTeam team;

    @SuppressWarnings("unused")
    public boolean getBlockOnHigh() {
        return blockOnHigh;
    }

    @SuppressWarnings("unused")
    public boolean getBlockOnMedium() {
        return blockOnMedium;
    }

    @SuppressWarnings("unused")
    public boolean getBlockOnLow() {
        return blockOnLow;
    }

    String getResultsHtml() {
        return resultsHtml;
    }

    List<ContextTestResult> getRunResults() {
        return runResults;
    }

    CloudCoreoTeam getTeam() {
        return team;
    }

    @DataBoundConstructor
    public CloudCoreoResultArchiver(boolean blockOnHigh, boolean blockOnMedium, boolean blockOnLow) {
        this.blockOnHigh = blockOnHigh;
        this.blockOnMedium = blockOnMedium;
        this.blockOnLow = blockOnLow;
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

    private String buildRowHtml(String name, String category, String displayName, String level, String violator, String link) {
        return "<tr>" +
                "<td><a href=\"" +
                link +
                "\">" +
                name +
                "</a></td>" +
                "<td>" +
                category +
                "</td>" +
                "<td>" +
                displayName +
                "</td>" +
                "<td>" +
                level +
                "</td>" +
                "<td>" +
                violator +
                "</td>" +
                "</tr>";
    }

    void writeResultsHtml(FilePath path, String buildId) {
        Map<String, ArrayList<ContextTestResult>> sortedViolations = new HashMap<>();
        for (ContextTestResult violation : getRunResults()) {
            ArrayList<ContextTestResult> currentList = sortedViolations.get(violation.getLevel());
            if (currentList == null) {
                currentList = new ArrayList<>();
            }
            currentList.add(violation);
            sortedViolations.put(violation.getLevel(), currentList);
        }
        StringBuilder allTableSections = new StringBuilder();
        for (String key : new HashSet<>(sortedViolations.keySet())) {
            allTableSections.append(buildTableSection(key, sortedViolations.get(key)));
        }

        File file = new File(path.getBaseName().replaceAll(" ", "\\\\ ") + "/" + buildId + ".xml");
        resultsHtml = buildViolatorTableHtml(allTableSections.toString());
        try {
            FileWriter f = new FileWriter(file);
            f.write(resultsHtml);
            f.close();
        } catch (IOException ex) {
            log.info(ex.getMessage());
        }
    }

    private String buildTableSection(String level, ArrayList<ContextTestResult> violationList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr><td align='center' colspan=\"5\">");
        sb.append(level);
        sb.append(" Violations</td>");
        sb.append("</tr>");
        for (ContextTestResult violation : violationList) {
            for (int x = 0; x < violation.getViolatingObjects().size(); x++) {
                String violator = violation.getViolatingObjects().get(x);
                sb.append(
                        buildRowHtml(violation.getName(), violation.getCategory(), violation.getDisplayName(),
                                violation.getLevel(), violator, violation.getLink())
                );
            }
        }
//        sb.append("<tr><td align='center' colspan='5'></td></tr>");
        return sb.toString();
    }

    private String buildViolatorTableHtml(String allTableSections) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<section name=\"Violation Summary\" line=\"0\" column=\"0\">\n" +
                "<table sorttable=\"yes\">" +
                allTableSections +
                "\n</table>\n" +
                "</section>";
    }

    private void printViolatorRow(ContextTestResult ctr, PrintStream consoleLogger) {

        for (int x = 0; x < ctr.getViolatingObjects().size(); x++) {
            String violator = ctr.getViolatingObjects().get(x);
            String sb = ctr.getName() +
                    " | " +
                    ctr.getCategory() +
                    " | " +
                    ctr.getDisplayName() +
                    " | " +
                    ctr.getLevel() +
                    " | " +
                    violator +
                    " | " +
                    ctr.getLink();
            consoleLogger.println(sb);
        }
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

        PrintStream logger = listener.getLogger();
        Map<String, String> vars;

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
            outputMessage(logger, message);
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
        if (!shouldBlockBuild()) {
            // no blocking for failures requested
            return;
        }

        try {
            waitForContextRun(logger, build);
        } catch (NullPointerException e) {
            String message = "\n>> Lost connection to CloudCoreo server, skipping DeployTime analysis.\n";
            outputMessage(logger, message);
            return;
        } catch (ExecutionFailedException e) {
            outputMessage(logger, e.getMessage());
            return;
        }

        try {
            runResults = getTeam().getDeployTime().getResults();
        } catch (Exception e) {
            String message = "\n>> There was a problem getting results, please contact us and share the following info:";
            outputMessage(logger, message);
            outputMessage(logger, e.getMessage());
            outputMessage(logger, Arrays.toString(e.getStackTrace()) + "\n");
            return;
        }

        writeResultsHtml(workspace, build.getId());

        reportResults(logger);

        if (hasBlockingFailures()) {
            build.setResult(Result.FAILURE);
        }
    }

    void reportResults(PrintStream logger) {
        String lineDelimiter = "\n**************************************************\n";
        String[] reportLevels = {"HIGH", "MEDIUM", "LOW"};

        if (getRunResults().size() > 0) {
            logger.println(lineDelimiter);
            logger.println(">>>> CloudCoreo Violations Found");
            logger.println(lineDelimiter);

            reportResultLevel(logger, reportLevels);

            logger.println(lineDelimiter);
            logger.println(">>>> End Violations Found");
            logger.println(lineDelimiter);
        } else {
            logger.println(lineDelimiter);
            logger.println(">>>> No CloudCoreo violations found for context");
            logger.println(lineDelimiter);
        }
    }

    // TODO: Store into a string and output string instead of iterating over runResults multiple times
    private void reportResultLevel(PrintStream logger, String[] levels) {
        boolean printedHeader = false;
        for (String level : levels) {
            for (ContextTestResult runResult : getRunResults()) {
                boolean matchedLevel = runResult.getLevel().equalsIgnoreCase(level);
                if (matchedLevel) {
                    if (!printedHeader) {
                        logger.println("** Violations with level: '" + level + "'");
                        printedHeader = true;
                    }
                    printViolatorRow(runResult, logger);
                }
            }
        }
    }

    private void waitForContextRun(PrintStream consoleLogger, Run<?, ?> build) throws ExecutionFailedException {
        String msg;
        boolean runHasTimedOut;
        boolean hasRunningJobs;
        do {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                msg = "Build aborted";
                build.setResult(Result.FAILURE);
                outputMessage(consoleLogger, msg);
                return;
            }

            runHasTimedOut = getTeam().getDeployTime().contextRunTimedOut();
            hasRunningJobs = getTeam().getDeployTime().hasRunningJobs();

            if (hasRunningJobs) {
                msg = "Please wait... The job is currently executing against your cloud objects...";
            } else {
                msg = "Please wait... CloudCoreo is initializing the analysis job...";
            }
            outputMessage(consoleLogger, msg);
        } while (!runHasTimedOut || hasRunningJobs);
        msg = "Finalizing the report...";
        outputMessage(consoleLogger, msg);
    }

    private void outputMessage(PrintStream consoleLogger, String message) {
        consoleLogger.println(message);
        consoleLogger.flush();
        log.info(message);
    }

    private boolean shouldBlockBuild() {
        return getBlockOnHigh() || getBlockOnMedium() || getBlockOnLow();
    }

    private boolean levelShouldBlock(String level) {
        level = level.toUpperCase();
        return level.equals("HIGH") || level.equals("MEDIUM") || level.equals("LOW");
    }

    boolean hasBlockingFailures() {
        for (ContextTestResult rr : getRunResults()) {
            if (shouldBlockBuild() && levelShouldBlock(rr.getLevel())) {
                return true;
            }
        }
        return false;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
