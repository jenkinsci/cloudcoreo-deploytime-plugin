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
    static final FilePath PATH = new FilePath(new File("/tmp/"));
    private CloudCoreoTeam team;
    private SimpleBuildWrapper.Disposer disposer;
    private Run<?, ?> build;

    @Before
    public void setUp() throws EndpointUnavailableException, IOException {
        team = new CloudCoreoTeamTest.CloudCoreoTeamStub();
        team.getDeployTime().setDeployTimeId("myContext", "myTask");
        disposer = new CloudCoreoDisposer(null, null, null, team);
        build = new CloudCoreoPublisherTest.BuildStub(rule);
    }

    @Test
    public void tearDownContextDisposer() throws IOException, InterruptedException {
        disposer.tearDown(build, PATH, null, new CloudCoreoPublisherTest.ListenerStub());
        Assert.assertTrue(team.isAvailable());
    }
}
