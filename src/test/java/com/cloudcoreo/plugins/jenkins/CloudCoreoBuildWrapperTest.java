package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class CloudCoreoBuildWrapperTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();

    private static final String CONTEXT = "myContext";
    private CloudCoreoBuildWrapperStub buildWrapperStub;

    public class CloudCoreoBuildWrapperStub extends CloudCoreoBuildWrapper {

        CloudCoreoBuildWrapperStub() {
            super("myTeamName", CloudCoreoBuildWrapperTest.CONTEXT);
        }

        @Override
        CloudCoreoTeam getTeam() {
            return new CloudCoreoTeamTest.CloudCoreoTeamStub();
        }
    }

    @Before
    public void setUpDescriptor() throws Descriptor.FormException {
        buildWrapperStub = new CloudCoreoBuildWrapperStub();
    }

    @Test
    public void ensureTeamCollection() throws EndpointUnavailableException {
        CloudCoreoBuildWrapper.DescriptorImpl descriptor = new CloudCoreoBuildWrapper.DescriptorImpl();
        descriptor.collectTeams(getTeamsJSON());
        Assert.assertEquals(descriptor.getTeams().size(), 1);
    }

    @Test
    public void runSetUp() throws IOException {
        SimpleBuildWrapper.Context context = new SimpleBuildWrapper.Context();
        CloudCoreoPublisherTest.BuildStub build = new CloudCoreoPublisherTest.BuildStub(rule);
        FilePath path = CloudCoreoDisposerTest.PATH;
        TaskListener listener = new CloudCoreoPublisherTest.ListenerStub();
        EnvVars env = new EnvVars();

        buildWrapperStub.setUp(context, build, path, null, listener, env);
        Assert.assertTrue(buildWrapperStub.getTeam().isAvailable());
    }

    private JSONObject getTeamsJSON() {
        JSONArray teamsArray = new JSONArray();
        JSONObject params = CloudCoreoTeamTest.SETUP_PARAMS;
        teamsArray.add(params);
        JSONObject json = new JSONObject();
        json.put("team", teamsArray);
        return json;
    }
}
