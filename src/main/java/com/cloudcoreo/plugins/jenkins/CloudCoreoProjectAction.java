package com.cloudcoreo.plugins.jenkins;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;

public class CloudCoreoProjectAction implements Action {

    private final AbstractProject<?, ?> project;
    private final Job<?, ?> job;

    public CloudCoreoProjectAction(AbstractProject<?, ?> project) {
        this((Job) project);
    }

    public CloudCoreoProjectAction(Job<?, ?> job) {
        this.job = job;
        project = job instanceof AbstractProject ? (AbstractProject) job : null;
    }

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
}
