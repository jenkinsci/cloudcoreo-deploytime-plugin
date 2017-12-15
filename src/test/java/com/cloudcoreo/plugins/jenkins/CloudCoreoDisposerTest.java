package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
import hudson.FilePath;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;

public class CloudCoreoDisposerTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();
    private CloudCoreoTeam team;
    private SimpleBuildWrapper.Disposer disposer;
    private Run<?, ?> build;

    static class DisposerStub extends CloudCoreoDisposer {
        DisposerStub(CloudCoreoTeam team) {
            super(null, null, null, team);
        }

        @Override
        String getBuildDirectory(Run<?, ?> build) {
            return "/tmp/jenkins/";
        }
    }

    @Before
    public void setUp() throws EndpointUnavailableException, IOException {
        team = new CloudCoreoTeamTest.CloudCoreoTeamStub();
        team.getDeployTime().setDeployTimeId("myContext", "myTask");
        disposer = new DisposerStub(team);
        build = new CloudCoreoPublisherTest.BuildStub(rule);
    }

    @Test
    public void tearDownContextDisposer() throws IOException, InterruptedException {
        disposer.tearDown(build, null, null, new CloudCoreoPublisherTest.ListenerStub());
        Assert.assertTrue(team.isAvailable());
    }
}
