package com.cloudcoreo.plugins.jenkins;


import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

class CloudCoreoDisposer extends SimpleBuildWrapper.Disposer {

    private final static Logger log = Logger.getLogger(CloudCoreoDisposer.class.getName());
    private static final long serialVersionUID = 2636645815974839783L;
    private final String disposerContext;
    private final String taskId;
    private final CloudCoreoTeam team;

    @SuppressWarnings("unused")
    private transient final SimpleBuildWrapper.Context context;


    CloudCoreoDisposer(SimpleBuildWrapper.Context context, String disposerContext, String taskId, CloudCoreoTeam team) {
        this.disposerContext = disposerContext;
        this.taskId = taskId;
        this.team = team;
        this.context = context;
    }

    @Override
    public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        log.info("finishing CloudCoreo analysis");

        Map<String, String> vars = new HashMap<>();
        vars.put("ccTask", taskId);
        vars.put("ccContext", disposerContext);

        try {
            team.getDeployTime().sendStopContext();
            vars.put("ccTeam", team.toString());
            writeSerializedDataToTempFile(workspace, vars, build.getId());
        } catch (URISyntaxException | IOException | NullPointerException e) {
            String message = "\nThere was a problem in the teardown of the build, skipping DeployTime analysis\n";
            listener.getLogger().println(message);
            team.makeUnavailable();
        }
    }

    static void writeSerializedDataToTempFile(FilePath path, Map<String, String> vars, String buildId)
            throws URISyntaxException, IOException {
        //create a temp file
        String fp = "file:///" + path + "/" + buildId + ".ser";
        File file = new File(new URI(fp.replaceAll(" ", "%20")).getPath());
        FileOutputStream f = new FileOutputStream(file);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(vars);
        s.close();
    }
}