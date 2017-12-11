package com.cloudcoreo.plugins.jenkins;

import hudson.model.Action;

public class CloudCoreoAbstractAction implements Action {
    public static final String URL_NAME = "cloudcoreo-deploytime";
    public static final String ICON_NAME = "cc-icon.gif";
    public static final String DISPLAY_NAME = "CloudCoreo DeployTime Results";

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
}
