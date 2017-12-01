package com.cloudcoreo.plugins.jenkins;

import hudson.model.FreeStyleBuild;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class CloudCoreoBuildActionTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();
    private FreeStyleBuild build;

    @Before
    public void setUp() throws IOException {
        build = new CloudCoreoPublisherTest.BuildStub(rule);
    }

    @Test
    public void buildActionCreated() {
        CloudCoreoBuildAction action = new CloudCoreoBuildAction(build, new JSONObject());
        Assert.assertNotNull(action.getDisplayName());
        Assert.assertNotNull(action.getIconFileName());
        Assert.assertNotNull(action.getUrlName());
        Assert.assertNotNull(action.getTarget());
    }
}
