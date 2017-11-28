package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.util.Base64;
import java.util.logging.Logger;


/**
 * Created by paul.allen on 8/10/17.
 */
@ExportedBean
public class CloudCoreoTeam implements Serializable {

    private static final long serialVersionUID = -8749212586752837178L;
    public static final Logger log = Logger.getLogger(CloudCoreoTeam.class.getName());

    private DeployTime deployTime = null;

    private final int domainPort;
    private boolean isAvailable;
    private final String teamName;
    private final String teamId;
    private final String teamKeyId;
    private final String teamSecretKey;
    private final String domain;
    private final String domainProtocol;

    @SuppressWarnings("unused")
    public String getTeamName() {
        return teamName;
    }

    @SuppressWarnings("unused")
    public String getTeamId() {
        return teamId;
    }

    @SuppressWarnings("unused")
    public String getTeamKeyId() {
        return teamKeyId;
    }

    @SuppressWarnings("unused")
    public String getTeamSecretKey() {
        return teamSecretKey;
    }

    @SuppressWarnings("unused")
    public String getDomain() {
        return deployTime.getDomain();
    }

    @SuppressWarnings("unused")
    public String getDomainProtocol() {
        return deployTime.getDomainProtocol();
    }

    @SuppressWarnings("unused")
    public int getDomainPort() {
        return deployTime.getDomainPort();
    }

    @SuppressWarnings("WeakerAccess")
    public DeployTime getDeployTime() {
        if (deployTime == null) {
            deployTime = loadNewDeployTime();
        }
        return deployTime;
    }

    CloudCoreoTeam(JSONObject json) {
        teamId = json.getString("teamId");
        teamName = json.getString("teamName");
        teamKeyId = json.getString("teamKeyId");
        teamSecretKey = json.getString("teamSecretKey");
        domain = json.getString("domain");
        domainProtocol = json.getString("domainProtocol");
        domainPort = json.getInt("domainPort");
    }

    @DataBoundConstructor
    public CloudCoreoTeam(String domain, String domainProtocol, int domainPort, String teamName, String teamId,
                          String teamKeyId, String teamSecretKey) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamKeyId = teamKeyId;
        this.teamSecretKey = teamSecretKey;
        this.domain = domain;
        this.domainPort = domainPort;
        this.domainProtocol = domainProtocol;
    }

    static CloudCoreoTeam getTeamFromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
        CloudCoreoTeam team = (CloudCoreoTeam) objectInputStream.readObject();
        objectInputStream.close();
        return team;
    }

    void makeAvailable() {
        isAvailable = true;
    }

    void makeUnavailable() {
        isAvailable = false;
    }

    boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Write the object to a Base64 string.
     */
    @Override
    public String toString() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (IOException ioe) {
            log.info(ioe.getMessage());
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    boolean equals(CloudCoreoTeam comparedTeam) {
        return this.domain.equals(comparedTeam.getDomain())
                && this.domainPort == comparedTeam.getDomainPort()
                && this.domainProtocol.equals(comparedTeam.getDomainProtocol())
                && this.teamId.equals(comparedTeam.getTeamId())
                && this.teamKeyId.equals(comparedTeam.getTeamKeyId())
                && this.teamSecretKey.equals(comparedTeam.getTeamSecretKey())
                && this.teamName.equals(comparedTeam.getTeamName());
    }

    DeployTime loadNewDeployTime() {
        deployTime = new DeployTime(teamId, domain, domainPort, domainProtocol, teamKeyId, teamSecretKey);
        return deployTime;
    }
}

