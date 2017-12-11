package com.cloudcoreo.plugins.jenkins;

import hudson.model.Action;
import hudson.model.Run;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerProxy;

import java.io.Serializable;

public class CloudCoreoBuildAction extends CloudCoreoAbstractAction implements Action, Serializable, StaplerProxy {

    private static final long serialVersionUID = 520981690971849654L;

    private final JSONObject result;
    private final transient Run build;

    public CloudCoreoBuildAction(Run build, JSONObject result) {
        super();
        this.build = build;
        this.result = result;
    }

    @Override
    public Object getTarget() {
        return this.result;
    }
}
