package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.Serializable;

class DeployTimeObject implements Serializable {
    private static final long serialVersionUID = 8945530772641303762L;

    private final String deployTimeUrl;
    private final String deployTimeId;
    private final String context;
    private final String task;
    private final String id;
    private Link team;
    private Link start;
    private Link stop;
    private Link results;
    private Link status;

    String getDeployTimeUrl() {
        return deployTimeUrl;
    }

    String getDeployTimeId() {
        return deployTimeId;
    }

    String getId() {
        return id;
    }

    String getContext() {
        return context;
    }

    String getTask() {
        return task;
    }

    Link getTeam() {
        return team;
    }

    Link getStart() {
        return start;
    }

    Link getStop() {
        return stop;
    }

    Link getResults() {
        return results;
    }

    Link getStatus() {
        return status;
    }

    DeployTimeObject(JSONObject deployTimeJSON){
        team = null;
        start = null;
        stop = null;
        results = null;
        status = null;

        deployTimeUrl = deployTimeJSON.getString("devTimeUrl");
        deployTimeId = deployTimeJSON.getString("devTimeId");
        context = deployTimeJSON.getString("context");
        task = deployTimeJSON.getString("task");
        id = deployTimeJSON.getString("id");

        JSONArray links = deployTimeJSON.getJSONArray("links");
        JSONObject link;
        for(int x = 0; x < links.size(); x++){
            link = links.getJSONObject(x);
            if(objectContainsKey(link, "team")) team = new Link(link);
            if(objectContainsKey(link, "start")) start = new Link(link);
            if(objectContainsKey(link, "stop")) stop = new Link(link);
            if(objectContainsKey(link, "results")) results = new Link(link);
            if(objectContainsKey(link, "status")) status = new Link(link);
        }
    }

    private boolean objectContainsKey(JSONObject obj, String key) {
        return obj.getString("ref").equalsIgnoreCase(key);
    }
}
