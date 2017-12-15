package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
import hudson.FilePath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;

public class ResultManagerTest {

    private String buildID;
    private ResultManager manager;
    private FilePath testFilePath;
    private CloudCoreoTeam team;

    static class ResultManagerStub extends ResultManager {
        ResultManagerStub(boolean blockOnLow, boolean blockOnMedium, boolean blockOnHigh) {
            super(blockOnLow, blockOnMedium, blockOnHigh, System.out);
        }
    }

    @Before
    public void setUp() throws EndpointUnavailableException, IOException {
        buildID = "1";
        team = new CloudCoreoTeamTest.CloudCoreoTeamStub();
        team.getDeployTime().setDeployTimeId("myContext", "myTask");

        manager = new ResultManagerStub(false, true, true);
        manager.setResults(team, buildID);

        testFilePath = CloudCoreoPublisherTest.BUILD_PATH;
        manager.writeResultsToFile(testFilePath, buildID);
    }

    @Test
    public void ensureAccurateBlocking() {
        ResultManager blockedManager = new ResultManagerStub(true, false, true);
        blockedManager.setResults(team, buildID);
        Assert.assertFalse(manager.hasBlockingFailures());
        Assert.assertTrue(blockedManager.hasBlockingFailures());
    }

    @Test
    public void resultsCanBeRetrievedFromFile() throws IOException {
        Assert.assertEquals(ResultManager.getAllResults(testFilePath).size(), 1);
        Assert.assertNotNull(ResultManager.getLastResult(testFilePath));
    }

    @Test
    public void canReportResultsToConsole() {
        manager.reportResultsToConsole();
        Assert.assertFalse(System.out.checkError());
    }
}
