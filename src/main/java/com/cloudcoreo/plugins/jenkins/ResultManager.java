package com.cloudcoreo.plugins.jenkins;

import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

class ResultManager {

    private boolean shouldBlockOnLow;
    private boolean shouldBlockOnMedium;
    private boolean shouldBlockOnHigh;
    private Path cloudCoreoFilePath;
    private List<ContextTestResult> results;
    private JSONObject resultsJSON;
    private PrintStream logger;

    ResultManager(boolean blockOnLow, boolean blockOnMedium, boolean blockOnHigh, PrintStream logger) {
        cloudCoreoFilePath = null;
        resultsJSON = new JSONObject();
        shouldBlockOnLow = blockOnLow;
        shouldBlockOnMedium = blockOnMedium;
        shouldBlockOnHigh = blockOnHigh;
        this.logger = logger;
    }

    private List<ContextTestResult> getRunResults() {
        return results;
    }

    void setResults(CloudCoreoTeam team) {
        results = team.getDeployTime().getResults();
        setResultsJSON();
    }

    void writeResultsToFile(FilePath filePath, String buildId) throws IOException {
        Path dirName = getCloudCoreoFilePath(filePath);
        String pathName = dirName + "/" + buildId + ".txt";

        if (!Files.exists(dirName)) {
            Files.createDirectory(dirName);
        }
        FileWriter file = new FileWriter(pathName);
        file.write(resultsJSON.toString());
        file.close();
    }

    void reportResultsToConsole() {
        String lineDelimiter = "**************************************************";

        logger.println("\n" + lineDelimiter);
        if (getRunResults().size() > 0) {
            logger.println(">>>> CloudCoreo Violations Found");
            logger.println(lineDelimiter + "\n");

            reportResultLevel();

            logger.println("\n" + lineDelimiter);
            logger.println(">>>> End Violations Found");
        } else {
            logger.println(">>>> No CloudCoreo violations found for context");
        }
        logger.println(lineDelimiter + "\n");
    }

    boolean hasBlockingFailures() {
        Set<?> levels = resultsJSON.keySet();
        for (Object level : levels) {
            String levelString = (String) level;
            if (shouldBlockBuild() && levelShouldBlock(levelString)) {
                return true;
            }
        }
        return false;
    }

    boolean shouldBlockBuild() {
        return shouldBlockOnLow || shouldBlockOnMedium || shouldBlockOnHigh;
    }

    private void setResultsJSON() {
        for (ContextTestResult violation : getRunResults()) {
            JSONArray currentList = (JSONArray) resultsJSON.get(violation.getLevel());
            if (currentList == null) {
                currentList = new JSONArray();
            }
            currentList.add(violation.getJSONResults());
            resultsJSON.put(violation.getLevel(), currentList);
        }
    }

    // TODO: Store into a string and output string instead of iterating over runResults multiple times
    private void reportResultLevel() {
        Set<?> levels = resultsJSON.keySet();
        for (Object level : levels) {
            String levelString = (String) level;
            boolean printedHeader = false;
            JSONArray levelResults = resultsJSON.getJSONArray(levelString);
            for (Object levelResult : levelResults) {
                if (!printedHeader) {
                    logger.println("** Violations with level: '" + levelString + "'");
                    printedHeader = true;
                }
                printViolatorRow((JSONObject) levelResult);
            }
        }
    }

    private void printViolatorRow(JSONObject contextResult) {
        JSONArray violatingObjects = contextResult.getJSONArray("violatingObjects");
        for (int x = 0; x < violatingObjects.size(); x++) {
            String violator = violatingObjects.getString(x);
            String sb = contextResult.getString("name") +
                    " | " +
                    contextResult.getString("category") +
                    " | " +
                    contextResult.getString("displayName") +
                    " | " +
                    contextResult.getString("level") +
                    " | " +
                    violator +
                    " | " +
                    contextResult.getString("link");
            logger.println(sb);
        }
    }

    private Path getCloudCoreoFilePath(FilePath filePath) {
        if (cloudCoreoFilePath == null) {
            cloudCoreoFilePath = Paths.get(filePath.getRemote().replaceAll(" ", "\\\\ ") + "/cloudcoreo/");
        }
        return cloudCoreoFilePath;
    }

    private boolean levelShouldBlock(String level) {
        return (level.equals("HIGH") && shouldBlockOnHigh)
                || (level.equals("MEDIUM") && shouldBlockOnMedium)
                || (level.equals("LOW") && shouldBlockOnLow);
    }
}
