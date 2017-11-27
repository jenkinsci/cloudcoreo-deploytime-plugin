package com.cloudcoreo.plugins.jenkins;

import hudson.model.Action;
import hudson.model.Run;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerProxy;

import java.io.Serializable;

public class CloudCoreoBuildAction implements Action, Serializable, StaplerProxy {

    private static final long serialVersionUID = 520981690971849654L;
    public static final String URL_NAME = "cloudcoreo-deploytime";
    public static final String ICON_NAME = null;
    public static final String DISPLAY_NAME = "CloudCoreo DeployTime Results";

    private final JSONObject result;
    private final transient Run build;

    public CloudCoreoBuildAction(Run build, JSONObject result) {
        super();
        this.build = build;
        this.result = result;
    }

    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    @Override
    public Object getTarget() {
        return this.result;
    }
}
