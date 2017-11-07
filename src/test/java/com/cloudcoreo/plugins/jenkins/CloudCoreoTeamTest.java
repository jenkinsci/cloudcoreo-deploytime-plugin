package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings("unused")
public class CloudCoreoTeamTest {

    private CloudCoreoTeam team;
    static final JSONObject SETUP_PARAMS = JSONObject.fromObject(
            "{" +
                "teamId: 'myTeamId'," +
                "teamName: 'myTeamName'," +
                "teamKeyId: 'myTeamKeyId'," +
                "teamSecretKey: 'myTeamSecretKey'," +
                "domain: 'myDomain'," +
                "domainProtocol: 'myDomainProtocol'," +
                "domainPort: 0" +
            "}"
    );

    @Before
    public void setUpTeam() {
        team = new CloudCoreoTeam(SETUP_PARAMS);
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
