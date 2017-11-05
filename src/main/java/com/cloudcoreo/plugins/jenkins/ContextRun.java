package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Created by paul.allen on 8/23/17.
 */
class ContextRun implements Serializable {
    private static final long serialVersionUID = 3296524026185644805L;
    private boolean hasRunningJobs;
    private String runningState;
    private String engineState;
    private String engineStatus;

    ContextRun(JSONObject status) {
        runningState = status.getString("runningState");
        engineState = status.getString("engineState");
        engineStatus = status.getString("engineStatus");
        hasRunningJobs = runningState.equalsIgnoreCase("running");
    }

    String getRunningState() {
        return runningState;
    }

    String getEngineState() {
        return engineState;
    }

    String getEngineStatus() {
        return engineStatus;
    }

    boolean hasRunningJobs() {
        return hasRunningJobs;
    }

    boolean hasExecutionFailed() {
        Pattern errorRegex = Pattern.compile("^([A-Z]+_ERROR|TERMINATED)$");
        return errorRegex.matcher(engineStatus).matches();
    }
}
