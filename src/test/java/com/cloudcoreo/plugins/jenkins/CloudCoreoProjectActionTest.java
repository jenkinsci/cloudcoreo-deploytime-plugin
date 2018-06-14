package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import java.io.IOException;

public class CloudCoreoProjectActionTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();
    private CloudCoreoProjectAction projectAction;

    private class ProjectStub extends CloudCoreoProjectAction {
        ProjectStub(AbstractProject<?, ?> project) {
            super(project);
        }

        @Override
        FilePath getResultsPath() {
            return CloudCoreoPublisherTest.BUILD_PATH;
        }
    }

    @Before
    public void setUp() throws IOException, EndpointUnavailableException {
        AbstractProject project = new FreeStyleProject(null, "myProject");
        projectAction = new ProjectStub(project);

        CloudCoreoTeam team = new CloudCoreoTeamTest.CloudCoreoTeamStub();
        team.getDeployTime().setDeployTimeId("myContext", "myTask");

        ResultManager manager = new ResultManagerTest.ResultManagerStub(false, true, true);
        manager.setResults(team, ResultManagerTest.BUILD_ID);

        FilePath testFilePath = CloudCoreoPublisherTest.BUILD_PATH;
        manager.writeResultsToFile(testFilePath, ResultManagerTest.BUILD_ID);
    }

    @Test
    public void shouldHaveBuildResults() throws IOException {
        Assert.assertEquals(projectAction.getAllBuildResults().size(), 1);
    }

    @Test
    public void shouldHaveLowViolation() throws IOException {
        Assert.assertEquals(projectAction.getLastCount("LOW"), 1);
    }

    @Test
    public void totalShouldBeNonZero() throws IOException {
        Assert.assertEquals(projectAction.getLastTotalCount(), 1);
    }
}
