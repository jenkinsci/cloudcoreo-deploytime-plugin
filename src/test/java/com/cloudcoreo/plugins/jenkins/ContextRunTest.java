package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class ContextRunTest {

    JSONObject getStubbedStatus(String runState, String engineState, String engineStatus) {
        JSONObject status = new JSONObject();
        status.put("runningState", runState);
        status.put("engineState", engineState);
        status.put("engineStatus", engineStatus);
        return status;
    }

    @Test
    public void contextRunShouldBeInitialized() {
        String runState = "running";
        String engineState = "EXECUTING";
        String engineStatus = "OK";
        ContextRun contextRun = new ContextRun(getStubbedStatus(runState, engineState, engineStatus));
        Assert.assertEquals(contextRun.getEngineState(), engineState);
        Assert.assertEquals(contextRun.getEngineStatus(), engineStatus);
        Assert.assertEquals(contextRun.getRunningState(), runState);
    }

    @Test
    public void contextRunShouldDetectRunningJobs() {
        ContextRun contextRun = new ContextRun(getStubbedStatus("running", "EXECUTING", ""));
        Assert.assertTrue(contextRun.hasRunningJobs());
        contextRun = new ContextRun(getStubbedStatus("not running", "EXECUTING", ""));
        Assert.assertFalse(contextRun.hasRunningJobs());
    }

    @Test
    public void contextRunShouldDetectExecutionFailures() {
        ContextRun contextRun = new ContextRun(getStubbedStatus("", "EXECUTING", "EXECUTION_ERROR"));
        Assert.assertTrue(contextRun.hasExecutionFailed());
        contextRun = new ContextRun(getStubbedStatus("", "EXECUTING", "TERMINATED"));
        Assert.assertTrue(contextRun.hasExecutionFailed());
        contextRun = new ContextRun(getStubbedStatus("", "EXECUTING", "EXECUTING"));
        Assert.assertFalse(contextRun.hasExecutionFailed());
    }
}
