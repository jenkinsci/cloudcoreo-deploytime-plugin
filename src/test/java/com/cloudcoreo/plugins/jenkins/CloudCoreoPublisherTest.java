package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.*;
import hudson.remoting.Channel;
import hudson.util.LogTaskListener;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloudCoreoPublisherTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();
    private CloudCoreoPublisher publisher;
    private FreeStyleBuild build;

    private class CloudCoreoPublisherStub extends CloudCoreoPublisher {

        private CloudCoreoTeam team;

        CloudCoreoPublisherStub(boolean blockOnHigh, boolean blockOnMedium, boolean blockOnLow) {
            super(blockOnHigh, blockOnMedium, blockOnLow);
            team = null;
        }

        CloudCoreoTeam getTeam() {
            if (team == null) {
                team = new CloudCoreoTeamTest.CloudCoreoTeamStub();
            }
            return team;
        }

        @Override
        Map<String, String> readSerializedDataFromTempFile(String buildID) {
            Map<String, String> response = new HashMap<>();
            response.put("ccTeam", new CloudCoreoTeamTest.CloudCoreoTeamStub().toString());
            return response;
        }

        @Override
        ResultManager getResultManager() {
            return new ResultManagerTest.ResultManagerStub(getBlockOnLow(), getBlockOnMedium(), getBlockOnHigh());
        }

        @Override
        FilePath getWorkspacePath() {
            return new FilePath(new File("/tmp/"));
        }
    }

    static class BuildStub extends FreeStyleBuild {
        private final String BUILD_ID = "unittest";

        BuildStub(JenkinsRule rule) throws IOException {
            super(rule.createFreeStyleProject());
        }
        @Nonnull
        @Override
        public String getId() {
            return BUILD_ID;
        }
    }

    static class ListenerStub extends LogTaskListener {
        ListenerStub() {
            super(null, null);
        }
        @Override
        public PrintStream getLogger() {
            return System.out;
        }
    }

    @Before
    public void setUp() throws IOException, EndpointUnavailableException {
        publisher = new CloudCoreoPublisherStub(true, true, true);
        build = new BuildStub(rule);
        publisher.getTeam().getDeployTime().setDeployTimeId("myContext", "myTask");
    }

    @Test
    public void descriptorShouldHaveNameAndIsApplicable() {
        CloudCoreoPublisher.DescriptorImpl descriptor = new CloudCoreoPublisher.DescriptorImpl();
        Assert.assertNotNull(descriptor.getDisplayName());
        Assert.assertTrue(descriptor.isApplicable(null));
    }

    @Test
    public void initializeTeamWithoutException() {
        try {
            publisher.initializeTeam(build);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void retrieveAndSetResultsWithoutException() {
        try {
            publisher.retrieveAndSetResults(build.getId());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void writeResultsWithoutException() {
        try {
            publisher.writeResults(build);
        } catch (Exception e) {
            Assert.fail();
        }
    }
}