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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        try {
            team.getDeployTime().sendStopContext();
            writeSerializedDataToTempFile(team.toString(), build);
        } catch (URISyntaxException | IOException | NullPointerException e) {
            String message = "\nThere was a problem in the teardown of the build, skipping DeployTime analysis\n";
            listener.getLogger().println(message);
            team.makeUnavailable();
        }
    }

    static void writeSerializedDataToTempFile(String teamString, Run<?, ?> build)
            throws URISyntaxException, IOException {
        //create a temp file
        String directory = build.getParent().getBuildDir().toString().replaceAll(" ", "%20") + "/";
        Path fullPath = Paths.get(directory + build.getId() + "/team.ser");
        Files.write(fullPath, teamString.getBytes());
        removePreviousTempFileIfExists(build.getPreviousBuild(), directory);
    }

    private static void removePreviousTempFileIfExists(Run<?, ?> previousBuild, String fileDirectory)
            throws URISyntaxException, IOException {
        if (previousBuild != null) {
            Path filePath = Paths.get(fileDirectory + previousBuild.getId() + "/team.ser");
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }
    }
}