package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.http.client.HttpClient;

import java.io.IOException;

@SuppressWarnings("unused")
public class DeployTimeTest {

    static class DeployTimeStub extends DeployTime {

        private static final int DOMAIN_PORT = 0;
        private static final String TEAM_ID = "teamName";
        private static final String DOMAIN = "test.com";
        private static final String DOMAIN_PROTOCOL = "https";
        private static final String ACCESS_KEY_ID = "access-key-id";
        private static final String SECRET_ACCESS_KEY = "secret-access-key";
        private String url;

        DeployTimeStub() {
            super(TEAM_ID, DOMAIN, DOMAIN_PORT, DOMAIN_PROTOCOL, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        }

        @Override
        HttpResponse makeHttpCall(HttpClient client, HttpRequestBase call) throws IOException {
            url = call.getURI().toString();
            return null;
        }

        @Override
        String parseResponse(HttpResponse response) {
            if (url.equals(getDeployTimeURL())) {
                return getDeployTimeResponse();
            } else if (url.equals(getDeployTimeInstance().getStatus().getHref())) {
                return getRunningStatusResponse();
            } else if (url.equals(getDeployTimeInstance().getResults().getHref())){
                return getResultsResponse();
            }
            return "{}";
        }

        private String getDeployTimeResponse() {
            String[] linkProperties = {"results", "status", "start", "stop", "team"};
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            JSONObject linkItem;

            for (String property : linkProperties) {
                linkItem = new JSONObject();

                linkItem.put("ref", property);
                linkItem.put("href", property + "myhref");
                linkItem.put("method", "mymethod");

                jsonArray.add(linkItem);
            }

            jsonObject.put("devTimeUrl", "url");
            jsonObject.put("devTimeId", "someId");
            jsonObject.put("id", "someId");
            jsonObject.put("context", "mycontext");
            jsonObject.put("task", "mytask");
            jsonObject.put("links", jsonArray);

            return jsonObject.toString();
        }

        private String getRunningStatusResponse() {
            JSONObject result = new JSONObject();
            result.put("status", ContextRunTest.getStubbedStatus("running", "EXECUTING", "OK"));
            return result.toString();
        }

        private String getResultsResponse() {
            return JSONObject.fromObject("{\n" +
                    "  \"results\": {\n" +
                    "    \"s3-logging-disabled\": {\n" +
                    "      \"audit_objects\": null,\n" +
                    "      \"formulas\": null,\n" +
                    "      \"link\": \"http://kb.cloudcoreo.com/mydoc_s3-logging-disabled.html\",\n" +
                    "      \"description\": \"S3 bucket logging has not been enabled for the affected resource.\",\n" +
                    "      \"call_modifiers\": null,\n" +
                    "      \"report_index\": 1,\n" +
                    "      \"provider\": \"AWS\",\n" +
                    "      \"operators\": null,\n" +
                    "      \"meta_\": [\n" +
                    "        {\n" +
                    "          \"name\": \"meta_nist_171_id\",\n" +
                    "          \"value\": {\n" +
                    "            \"string\": \"3.1.2\"\n" +
                    "          }\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"raise_when\": null,\n" +
                    "      \"id\": \"1b4dac5ecc468016459b0e3cac5d075bd2b85507\",\n" +
                    "      \"timestamp\": \"2017-10-05T20:15:34.353Z\",\n" +
                    "      \"report_size\": 1,\n" +
                    "      \"suggested_action\": \"Enable logging on your S3 buckets.\",\n" +
                    "      \"id_map\": null,\n" +
                    "      \"level\": \"Low\",\n" +
                    "      \"display_name\": \"S3 bucket logging not enabled\",\n" +
                    "      \"name_id\": \"s3-logging-disabled_1b4dac5\",\n" +
                    "      \"service\": \"s3\",\n" +
                    "      \"name\": \"s3-logging-disabled\",\n" +
                    "      \"objectives\": null,\n" +
                    "      \"region\": \"us-east-1\",\n" +
                    "      \"category\": \"Audit\",\n" +
                    "      \"include_violations_in_count\": true,\n" +
                    "      \"violating_objects\": [\n" +
                    "        \"pallen-failer\"\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  }\n" +
                    "}").toString();
        }

        final int getTimeoutLimit() {
            return -1;
        }
    }

    private DeployTime deployTime;

    @Before
    public void setUpDeployTime() throws EndpointUnavailableException {
        deployTime = new DeployTimeStub();
        deployTime.setDeployTimeId("myContext", "myTask");
    }

    @Test
    public void deployTimeEndpointShouldBeSet() {
        Assert.assertNotNull(deployTime.getResults());
        Assert.assertEquals(deployTime.getDomain(), DeployTimeStub.DOMAIN);
        Assert.assertEquals(deployTime.getDomainPort(), DeployTimeStub.DOMAIN_PORT);
        Assert.assertEquals(deployTime.getDomainProtocol(), DeployTimeStub.DOMAIN_PROTOCOL);
    }

    @Test
    public void deployTimeInstanceShouldBeSet() {
        DeployTimeObject instance = deployTime.getDeployTimeInstance();
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getDeployTimeId());
        Assert.assertNotNull(instance.getStatus());
        Assert.assertNotNull(instance.getId());
        Assert.assertNotNull(instance.getResults());
        Assert.assertNotNull(instance.getTeam());
        Assert.assertNotNull(instance.getContext());
        Assert.assertNotNull(instance.getDeployTimeUrl());
        Assert.assertNotNull(instance.getStart());
        Assert.assertNotNull(instance.getStop());
        Assert.assertNotNull(instance.getTask());
    }

    @Test
    public void getResultsShouldBeOfSizeOne() {
        Assert.assertEquals(deployTime.getResults().size(), 1);
    }

    @Test
    public void contextHasRunningJobs() throws ExecutionFailedException {
        Assert.assertTrue(deployTime.hasRunningJobs());
    }

    @Test
    public void contextRunShouldTimeOut() {
        deployTime.sendStartContext();
        deployTime.sendStopContext();
        Assert.assertTrue(deployTime.contextRunTimedOut());
    }
}
