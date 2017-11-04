package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.ExecutionFailedException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class DeployTimeTest {

    class DeployTimeStub extends DeployTime {

        private static final int DOMAIN_PORT = 0;
        private static final String TEAM_ID = "teamName";
        private static final String DOMAIN = "test.com";
        private static final String DOMAIN_PROTOCOL = "https";
        private static final String ACCESS_KEY_ID = "access-key-id";
        private static final String SECRET_ACCESS_KEY = "secret-access-key";

        DeployTimeStub() {
            super(TEAM_ID, DOMAIN, DOMAIN_PORT, DOMAIN_PROTOCOL, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        }

        @Override
        JSONObject sendSignedRequest(String url, String callType, String body) {
            JSONObject response;
            if (url.equals(getDeployTimeURL())) {
                response = getDeployTimeResponse();
            } else if (url.equals(getDeployTimeInstance().getStatus().getHref())) {
                response = getRunningStatusResponse();
            } else {
                response = getResultsResponse();
            }
            return response;
        }

        private JSONObject getDeployTimeResponse() {
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            JSONObject resultsLink = new JSONObject();
            JSONObject statusLink = new JSONObject();

            resultsLink.put("ref", "results");
            resultsLink.put("href", "myhref");
            resultsLink.put("method", "mymethod");

            statusLink.put("ref", "status");
            statusLink.put("href", new ContextRunTest().getStubbedStatus("running", "OK"));
            statusLink.put("method", "mymethod");

            jsonArray.add(resultsLink);
            jsonArray.add(statusLink);

            jsonObject.put("devTimeUrl", "url");
            jsonObject.put("devTimeId", "someId");
            jsonObject.put("id", "someId");
            jsonObject.put("context", "mycontext");
            jsonObject.put("task", "mytask");
            jsonObject.put("links", jsonArray);

            return jsonObject;
        }

        private JSONObject getRunningStatusResponse() {
            JSONObject result = new JSONObject();
            result.put("status", new ContextRunTest().getStubbedStatus("running", "OK"));
            return result;
        }

        private JSONObject getResultsResponse() {
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
                    "}");
        }

        final int getTimeoutLimit() {
            return -1;
        }

        private String getDeployTimeURL() {
            return DOMAIN_PROTOCOL + "://" + DOMAIN + ":" + DOMAIN_PORT
                    + "/api/teams/" + TEAM_ID + "/devtime";
        }
    }
    private DeployTime deployTime;

    @Before
    public void setUpDeployTime() {
        deployTime = new DeployTimeStub();
        deployTime.setDeployTimeId("myContext", "myTask");
    }

    @Test
    public void deployTimeInstanceShouldBeSet() {
        Assert.assertNotNull(deployTime.getDeployTimeInstance());
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
