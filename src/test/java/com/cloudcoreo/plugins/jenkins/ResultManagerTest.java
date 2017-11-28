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

    @Before
    public void setUp() throws EndpointUnavailableException, IOException {
        buildID = "unittest";
        team = new CloudCoreoTeamTest.CloudCoreoTeamStub();
        team.getDeployTime().setDeployTimeId("myContext", "myTask");

        manager = new ResultManager(false, true, true, System.out);
        manager.setResults(team, buildID);

        testFilePath = new FilePath(new File("/tmp/"));
        manager.writeResultsToFile(testFilePath, buildID);
    }

    @Test
    public void ensureAccurateBlocking() {
        ResultManager blockedManager = new ResultManager(true, false, true, null);
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
