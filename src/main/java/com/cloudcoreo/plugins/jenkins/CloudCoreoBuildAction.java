package com.cloudcoreo.plugins.jenkins;

import hudson.model.Action;
import org.kohsuke.stapler.StaplerProxy;

import java.io.Serializable;

public class CloudCoreoBuildAction implements Action, Serializable, StaplerProxy {
    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public Object getTarget() {
        return null;
    }
}
