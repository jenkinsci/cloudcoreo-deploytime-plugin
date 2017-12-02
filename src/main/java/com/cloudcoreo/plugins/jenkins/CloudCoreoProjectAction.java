package com.cloudcoreo.plugins.jenkins;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Job;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class CloudCoreoProjectAction extends CloudCoreoAbstractAction {

    private final static int RESULT_LIMIT = 30;
    private final AbstractProject<?, ?> project;
    @SuppressWarnings("FieldCanBeLocal")
    private final Job<?, ?> job;
    private JSONArray results;
    private JSONObject lastResult;

    CloudCoreoProjectAction(AbstractProject<?, ?> project) {
        this((Job) project);
    }

    private CloudCoreoProjectAction(Job<?, ?> job) {
        this.job = job;
        project = (job instanceof AbstractProject) ? (AbstractProject) job : null;
        results = null;
        lastResult = new JSONObject();
    }

    @SuppressWarnings("WeakerAccess")
    public JSONArray getAllBuildResults() throws IOException {
        if (this.project == null) {
            return new JSONArray();
        }
        results = ResultManager.getAllResults(getWorkspacePath());
        if (results.size() > RESULT_LIMIT) {
            int lastIndex = results.size() - 1;
            results = JSONArray.fromObject(results.subList(lastIndex - RESULT_LIMIT, lastIndex));
        }
        return results;
    }

    @SuppressWarnings("WeakerAccess")
    public JSONObject getLastBuildResult() throws IOException {
        results = getAllBuildResults();
        if (results.size() > 0) {
            lastResult = results.getJSONObject(results.size() - 1);
        }

        return lastResult;
    }

    @SuppressWarnings("WeakerAccess")
    public int getLastCount(String level) throws IOException {
        JSONObject violations = getLastBuildResult().getJSONObject("violations");
        level = level.toUpperCase();
        if (violations.containsKey(level)) {
            return violations.getJSONArray(level).size();
        }
        return 0;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public int getLastTotalCount() throws IOException {
        String[] levels = {"LOW", "MEDIUM", "HIGH"};
        int total = 0;
        for (String level : levels) {
            total += getLastCount(level);
        }
        return total;
    }

    FilePath getWorkspacePath() {
        return this.project.getWorkspace();
    }
}
