package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class ContextRunTest {

    JSONObject getStubbedStatus(String runState, String engineStatus) {
        JSONObject status = new JSONObject();
        status.put("runningState", runState);
        status.put("engineState", "EXECUTING");
        status.put("engineStatus", engineStatus);
        return status;
    }

    @Test
    public void contextRunShouldDetectRunningJobs() {
        ContextRun contextRun = new ContextRun(getStubbedStatus("running", ""));
        Assert.assertTrue(contextRun.hasRunningJobs());
        contextRun = new ContextRun(getStubbedStatus("not running", ""));
        Assert.assertFalse(contextRun.hasRunningJobs());
    }

    @Test
    public void contextRunShouldDetectExecutionFailures() {
        ContextRun contextRun = new ContextRun(getStubbedStatus("", "EXECUTION_ERROR"));
        Assert.assertTrue(contextRun.hasExecutionFailed());
        contextRun = new ContextRun(getStubbedStatus("", "TERMINATED"));
        Assert.assertTrue(contextRun.hasExecutionFailed());
        contextRun = new ContextRun(getStubbedStatus("", "EXECUTING"));
        Assert.assertFalse(contextRun.hasExecutionFailed());
    }
}
