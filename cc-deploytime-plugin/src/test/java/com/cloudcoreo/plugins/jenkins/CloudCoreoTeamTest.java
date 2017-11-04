package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings("unused")
public class CloudCoreoTeamTest {

    private CloudCoreoTeam team;

    @Before
    public void setUpTeam() {
        JSONObject setupParams = new JSONObject();
        setupParams.put("teamId", "myTeamId");
        setupParams.put("teamName", "myTeamName");
        setupParams.put("teamKeyId", "myTeamKeyId");
        setupParams.put("teamSecretKey", "myTeamSecretKey");
        setupParams.put("domain", "myDomain");
        setupParams.put("domainProtocol", "myDomainProtocol");
        setupParams.put("domainPort", 0);
        team = new CloudCoreoTeam(setupParams);
    }

    @Test
    public void ensureDeployTimeLoadAndReload() {
        DeployTime loadedDeployTime = team.getDeployTime();
        team.reloadDeployTime();
        DeployTime reloadedDeployTime = team.getDeployTime();
        Assert.assertNotNull(loadedDeployTime);
        Assert.assertTrue(loadedDeployTime != reloadedDeployTime);
    }

    @Test
    public void ensureTeamAvailabilityToggle() {
        team.makeAvailable();
        Assert.assertTrue(team.isAvailable());
        team.makeUnavailable();
        Assert.assertFalse(team.isAvailable());
    }

    @Test
    public void encodeAndDecodeTeam() throws IOException, ClassNotFoundException {
        String encodedTeam = team.toString();
        CloudCoreoTeam decodedTeam = CloudCoreoTeam.getTeamFromString(encodedTeam);
        decodedTeam.reloadDeployTime();
        Assert.assertNotNull(encodedTeam);
        Assert.assertTrue(team.equals(decodedTeam));
    }
}
