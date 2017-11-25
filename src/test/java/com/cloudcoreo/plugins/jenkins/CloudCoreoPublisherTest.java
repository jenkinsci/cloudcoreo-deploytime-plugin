package com.cloudcoreo.plugins.jenkins;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class CloudCoreoPublisherTest {

    private class CloudCoreoPublisherStub extends CloudCoreoPublisher {

        CloudCoreoPublisherStub(boolean blockOnHigh, boolean blockOnMedium, boolean blockOnLow) {
            super(blockOnHigh, blockOnMedium, blockOnLow);
        }

        CloudCoreoTeam getTeam() {
            return new CloudCoreoTeam(CloudCoreoTeamTest.SETUP_PARAMS);
        }
    }

    @Test
    public void descriptorShouldHaveNameAndIsApplicable() {
        CloudCoreoPublisher.DescriptorImpl descriptor = new CloudCoreoPublisher.DescriptorImpl();
        Assert.assertNotNull(descriptor.getDisplayName());
        Assert.assertTrue(descriptor.isApplicable(null));
    }

}