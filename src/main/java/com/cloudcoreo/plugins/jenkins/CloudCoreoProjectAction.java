package com.cloudcoreo.plugins.jenkins;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;

public class CloudCoreoProjectAction implements Action {

    private final static int RESULT_LIMIT = 30;
    private final AbstractProject<?, ?> project;
    private final Job<?, ?> job;
    private JSONArray results;
    private JSONObject lastResult;

    public CloudCoreoProjectAction(AbstractProject<?, ?> project) {
        this((Job) project);
    }

    public CloudCoreoProjectAction(Job<?, ?> job) {
        this.job = job;
        project = (job instanceof AbstractProject) ? (AbstractProject) job : null;
        results = null;
        lastResult = new JSONObject();
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "CloudCoreo DeployTime Results";
    }

    @Override
    public String getUrlName() {
        return "cloudcoreo-deploytime";
    }

    public JSONArray getAllBuildResults() throws IOException {
        if (this.project == null) {
            return new JSONArray();
        }
        results = ResultManager.getAllResults(this.project.getWorkspace());
        if (results.size() > RESULT_LIMIT) {
            int lastIndex = results.size() - 1;
            results = JSONArray.fromObject(results.subList(lastIndex - RESULT_LIMIT, lastIndex));
        }
        return results;
    }

    public JSONObject getLastBuildResult() throws IOException {
        results = getAllBuildResults();
        if (results.size() > 0) {
            lastResult = results.getJSONObject(results.size() - 1);
        }

        return lastResult;
    }

    public int getLastCount(String level) throws IOException {
        JSONObject violations = getLastBuildResult().getJSONObject("violations");
        level = level.toUpperCase();
        if (violations.containsKey(level)) {
            return violations.getJSONArray(level).size();
        }
        return 0;
    }

    public int getLastTotalCount() throws IOException {
        String[] levels = {"LOW", "MEDIUM", "HIGH"};
        int total = 0;
        for (String level : levels) {
            total += getLastCount(level);
        }
        return total;
    }
}
