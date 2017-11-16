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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private List<String> resultsHtml;

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

    List<String> getResultsHtml() {
        return resultsHtml;
    }

    List<ContextTestResult> getRunResults() {
        return runResults;
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

    private List<String> buildRowHtml(ContextTestResult violation, String violator) {
        String[] columnContents = {
                "<a href=\"" + violation.getLink() + "\">" + violation.getName() + "</a>",
                violation.getCategory(),
                violation.getDisplayName(),
                violation.getLevel(),
                violator
        };
        List<String> row = new ArrayList<>();
        row.add("<tr>");
        for (String column : columnContents) {
            row.add("<td>");
            row.add(column);
            row.add("</td>");
        }
        row.add("</tr>");
        return row;
    }

    void writeResultsHtml(FilePath filePath, String buildId) {
        Map<String, ArrayList<ContextTestResult>> sortedViolations = new HashMap<>();
        for (ContextTestResult violation : getRunResults()) {
            ArrayList<ContextTestResult> currentList = sortedViolations.get(violation.getLevel());
            if (currentList == null) {
                currentList = new ArrayList<>();
            }
            currentList.add(violation);
            sortedViolations.put(violation.getLevel(), currentList);
        }
        List<String> allTableSections = new ArrayList<>();
        for (String key : new HashSet<>(sortedViolations.keySet())) {
            allTableSections.addAll(buildTableSection(key, sortedViolations.get(key)));
        }

        String pathName = filePath.getRemote().replaceAll(" ", "\\\\ ") + "/" + buildId + ".xml";
        resultsHtml = buildViolatorTableHtml(allTableSections);
        try {
            Files.write(Paths.get(pathName), resultsHtml, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            log.info(ex.getMessage());
        }
    }

    private List<String> buildTableSection(String level, ArrayList<ContextTestResult> violationList) {
        List<String> section = new ArrayList<>();
        section.add("<tr>");
        section.add("<td align='center' colspan=\"5\">");
        section.add(level + " Violations");
        section.add("</td>");
        section.add("</tr>");
        for (ContextTestResult violation : violationList) {
            for (int x = 0; x < violation.getViolatingObjects().size(); x++) {
                String violator = violation.getViolatingObjects().get(x);
                section.addAll(buildRowHtml(violation, violator));
            }
        }
//        sb.append("<tr><td align='center' colspan='5'></td></tr>");
        return section;
    }

    private List<String> buildViolatorTableHtml(List<String> allTableSections) {
        List<String> table = new ArrayList<>();
        table.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        table.add("<section name=\"Violation Summary\" line=\"0\" column=\"0\">");
        table.add("<table sorttable=\"yes\">");
        table.addAll(allTableSections);
        table.add("</table>");
        table.add("</section>");
        return table;
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
