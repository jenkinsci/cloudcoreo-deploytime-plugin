package com.cloudcoreo.plugins.jenkins;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CloudCoreoProjectActionTest {
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
    public void setUp() {
        AbstractProject project = new FreeStyleProject(null, "myProject");
        projectAction = new ProjectStub(project);
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
