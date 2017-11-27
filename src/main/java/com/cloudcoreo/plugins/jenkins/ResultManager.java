package com.cloudcoreo.plugins.jenkins;

import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class ResultManager {

    private boolean shouldBlockOnLow;
    private boolean shouldBlockOnMedium;
    private boolean shouldBlockOnHigh;
    private static Path cloudCoreoFilePath = null;
    private List<ContextTestResult> results;
    private JSONObject resultsJSON;
    private JSONObject violationsJSON;
    private PrintStream logger;

    ResultManager(boolean blockOnLow, boolean blockOnMedium, boolean blockOnHigh, PrintStream logger) {
        resultsJSON = new JSONObject();
        violationsJSON = new JSONObject();
        shouldBlockOnLow = blockOnLow;
        shouldBlockOnMedium = blockOnMedium;
        shouldBlockOnHigh = blockOnHigh;
        this.logger = logger;
    }

    private List<ContextTestResult> getRunResults() {
        return results;
    }

    void setResults(CloudCoreoTeam team, String buildID) {
        results = team.getDeployTime().getResults();
        setResultsJSON(buildID);
    }

    void writeResultsToFile(FilePath filePath, String buildId) throws IOException {
        Path dirName = getCloudCoreoFilePath(filePath);
        String pathName = dirName + "/" + buildId + ".txt";

        if (!Files.exists(dirName)) {
            Files.createDirectory(dirName);
        }
        Writer file = null;
        try {
            file = new OutputStreamWriter(new FileOutputStream(pathName), StandardCharsets.UTF_8);
            file.write(resultsJSON.toString());
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

    static JSONArray getAllResults(FilePath filePath) throws IOException {
        Path dirName = getCloudCoreoFilePath(filePath);
        List<Path> resultFiles = new ArrayList<>();
        JSONArray results = new JSONArray();
        Files.list(dirName).forEachOrdered(resultFiles::add);
        for (Path file : resultFiles) {
            String contents = Files.readAllLines(file).get(0);
            results.add(JSONObject.fromObject(contents));
        }
        return results;
    }

    static JSONObject getLastResult(FilePath filePath) throws IOException {
        JSONArray results = getAllResults(filePath);
        return results.getJSONObject(results.size() - 1);
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
        Set<?> levels = violationsJSON.keySet();
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

    private void setResultsJSON(String buildID) {
        resultsJSON.put("build", buildID);
        for (ContextTestResult violation : getRunResults()) {
            JSONArray currentList = (JSONArray) violationsJSON.get(violation.getLevel());
            if (currentList == null) {
                currentList = new JSONArray();
            }
            currentList.add(violation.getJSONResults());
            violationsJSON.put(violation.getLevel(), currentList);
        }
        resultsJSON.put("violations", violationsJSON);
    }

    private void reportResultLevel() {
        Set<?> levels = violationsJSON.keySet();
        for (Object level : levels) {
            String levelString = (String) level;
            boolean printedHeader = false;
            JSONArray levelResults = violationsJSON.getJSONArray(levelString);
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

    private static Path getCloudCoreoFilePath(FilePath filePath) {
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
