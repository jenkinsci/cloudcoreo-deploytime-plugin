package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class ContextTestResultTest {

    private JSONObject jsonObject;
    private JSONArray jsonArray;
    private JSONObject emptyJSONObject;

    static JSONArray getObjectArrayJson() {
        JSONArray result = new JSONArray();
        result.add(JSONObject.fromObject(
                "{" +
                "  \"ec2-vpc-flow-logs\": {" +
                "    \"audit_objects\": null," +
                "    \"call_modifiers\": null," +
                "    \"category\": \"Audit\"," +
                "    \"description\": \"VPC Flow Logs is a feature that enables you to capture information about the IP traffic going to and from network interfaces in your VPC. After you've created a flow log, you can view and retrieve its data in Amazon CloudWatch Logs.\"," +
                "    \"display_name\": \"Ensure VPC flow logging is enabled in all VPCs (Scored)\"," +
                "    \"formulas\": null," +
                "    \"id\": \"0e29276a61826f26b268243d3b8add840e29a558\"," +
                "    \"id_map\": null," +
                "    \"include_violations_in_count\": \"true\"," +
                "    \"level\": \"Low\"," +
                "    \"link\": \"http://kb.cloudcoreo.com/mydoc_ec2-vpc-flow-logs.html\"," +
                "    \"meta_\": [" +
                "      {" +
                "        \"name\": \"meta_cis_id\"," +
                "        \"value\": {" +
                "          \"string\": \"4.3\"" +
                "        }" +
                "      }," +
                "      {" +
                "        \"name\": \"meta_cis_scored\"," +
                "        \"value\": {" +
                "          \"string\": \"true\"" +
                "        }" +
                "      }," +
                "      {" +
                "        \"name\": \"meta_cis_level\"," +
                "        \"value\": {" +
                "          \"string\": \"1\"" +
                "        }" +
                "      }" +
                "    ]," +
                "    \"name\": \"ec2-vpc-flow-logs\"," +
                "    \"name_id\": \"ec2-vpc-flow-logs_0e29276\"," +
                "    \"objectives\": null," +
                "    \"operators\": null," +
                "    \"provider\": \"AWS\"," +
                "    \"raise_when\": null," +
                "    \"region\": \"us-east-1\"," +
                "    \"report_index\": 1," +
                "    \"report_size\": 1," +
                "    \"service\": \"ec2\"," +
                "    \"suggested_action\": \"VPC Flow Logs be enabled for packet 'Rejects' for VPCs.\"," +
                "    \"timestamp\": \"2017-07-21T17:50:40.569Z\"," +
                "    \"violating_objects\": [" +
                "      \"vpc-7e98f807\"" +
                "    ]" +
                "  }" +
                "}"));
        result.add(JSONObject.fromObject(
                "{" +
                "  \"anothertest\": {" +
                "    \"audit_objects\": null," +
                "    \"call_modifiers\": null," +
                "    \"category\": \"Audit\"," +
                "    \"description\": \"VPC Flow Logs is a feature that enables you to capture information about the IP traffic going to and from network interfaces in your VPC. After you've created a flow log, you can view and retrieve its data in Amazon CloudWatch Logs.\"," +
                "    \"display_name\": \"Ensure VPC flow logging is enabled in all VPCs (Scored)\"," +
                "    \"formulas\": null," +
                "    \"id\": \"0e29276a61826f26b268243d3b8add840e29a558\"," +
                "    \"id_map\": null," +
                "    \"include_violations_in_count\": \"true\"," +
                "    \"level\": \"Low\"," +
                "    \"link\": \"http://kb.cloudcoreo.com/mydoc_ec2-vpc-flow-logs.html\"," +
                "    \"meta_\": [" +
                "      {" +
                "        \"name\": \"meta_cis_id\"," +
                "        \"value\": {" +
                "          \"string\": \"4.3\"" +
                "        }" +
                "      }," +
                "      {" +
                "        \"name\": \"meta_cis_scored\"," +
                "        \"value\": {" +
                "          \"string\": \"true\"" +
                "        }" +
                "      }," +
                "      {" +
                "        \"name\": \"meta_cis_level\"," +
                "        \"value\": {" +
                "          \"string\": \"1\"" +
                "        }" +
                "      }" +
                "    ]," +
                "    \"name\": \"anothertest\"," +
                "    \"name_id\": \"ec2-vpc-flow-logs_0e29276\"," +
                "    \"objectives\": null," +
                "    \"operators\": null," +
                "    \"provider\": \"AWS\"," +
                "    \"raise_when\": null," +
                "    \"region\": \"us-east-1\"," +
                "    \"report_index\": 1," +
                "    \"report_size\": 1," +
                "    \"service\": \"ec2\"," +
                "    \"suggested_action\": \"VPC Flow Logs be enabled for packet 'Rejects' for VPCs.\"," +
                "    \"timestamp\": \"2017-07-21T17:50:40.569Z\"," +
                "    \"violating_objects\": [" +
                "      \"vpc-7e98f807\"," +
                "      \"vpc-hd7sdf80\"," +
                "      \"vpc-6e6362sd\"" +
                "    ]" +
                "  }" +
                "}"));
        return result;
    }

    static JSONObject getObjectJson() {
        return getObjectArrayJson().getJSONObject(0);
    }

    static JSONObject getRealObjectJson() {
        return JSONObject.fromObject(
                "{" +
                        "  \"ec2-unrestricted-traffic\": {" +
                        "    \"audit_objects\": null," +
                        "    \"formulas\": null," +
                        "    \"link\": \"http://kb.cloudcoreo.com/mydoc_ec2-unrestricted-traffic.html\"," +
                        "    \"description\": \"All IP addresses are allowed to access resources in a specific security group.\"," +
                        "    \"call_modifiers\": null," +
                        "    \"report_index\": 8," +
                        "    \"provider\": \"AWS\"," +
                        "    \"operators\": null," +
                        "    \"meta_\": [" +
                        "      {" +
                        "        \"name\": \"meta_nist_171_id\"," +
                        "        \"value\": {" +
                        "          \"string\": \"3.4.7\"" +
                        "        }" +
                        "      }" +
                        "    ]," +
                        "    \"raise_when\": null," +
                        "    \"id\": \"89e9809d5da89fb60a472b4464ea3c84cfa1e637\"," +
                        "    \"timestamp\": \"2017-08-30T19:36:47.755Z\"," +
                        "    \"report_size\": 16," +
                        "    \"suggested_action\": \"Restrict access to the minimum specific set of IP address or ports necessary.\"," +
                        "    \"id_map\": null," +
                        "    \"level\": \"Low\"," +
                        "    \"display_name\": \"Security group allows unrestricted traffic\"," +
                        "    \"name_id\": \"ec2-unrestricted-traffic_89e9809\"," +
                        "    \"service\": \"ec2\"," +
                        "    \"name\": \"ec2-unrestricted-traffic\"," +
                        "    \"objectives\": null," +
                        "    \"region\": \"us-east-1\"," +
                        "    \"category\": \"Security\"," +
                        "    \"include_violations_in_count\": true," +
                        "    \"violating_objects\": [" +
                        "      \"sg-7c135c0c\"," +
                        "      \"sg-bb135ccb\"," +
                        "      \"sg-fc37788c\"," +
                        "      \"sg-7b135c0b\"," +
                        "      \"sg-b52e61c5\"," +
                        "      \"sg-bc105fcc\"," +
                        "      \"sg-00105f70\"," +
                        "      \"sg-5e125d2e\"," +
                        "      \"sg-783b7408\"" +
                        "    ]" +
                        "  }," +
                        "  \"ec2-ports-range\": {" +
                        "    \"audit_objects\": null," +
                        "    \"formulas\": null," +
                        "    \"link\": \"http://kb.cloudcoreo.com/mydoc_ec2-ports-range.html\"," +
                        "    \"description\": \"Security group contains a port range rather than individual ports.\"," +
                        "    \"call_modifiers\": null," +
                        "    \"report_index\": 2," +
                        "    \"provider\": \"AWS\"," +
                        "    \"operators\": null," +
                        "    \"meta_\": [" +
                        "      {" +
                        "        \"name\": \"meta_nist_171_id\"," +
                        "        \"value\": {" +
                        "          \"string\": \"3.4.7\"" +
                        "        }" +
                        "      }" +
                        "    ]," +
                        "    \"raise_when\": null," +
                        "    \"id\": \"01341d010311299087719799ed8d662b918160f1\"," +
                        "    \"timestamp\": \"2017-08-30T19:36:47.578Z\"," +
                        "    \"report_size\": 16," +
                        "    \"suggested_action\": \"Only add rules to your Security group that specify individual ports and don't use port ranges unless they are required.\"," +
                        "    \"id_map\": null," +
                        "    \"level\": \"Low\"," +
                        "    \"display_name\": \"Security group contains a port range\"," +
                        "    \"name_id\": \"ec2-ports-range_01341d0\"," +
                        "    \"service\": \"ec2\"," +
                        "    \"name\": \"ec2-ports-range\"," +
                        "    \"objectives\": null," +
                        "    \"region\": \"us-east-1\"," +
                        "    \"category\": \"Security\"," +
                        "    \"include_violations_in_count\": true," +
                        "    \"violating_objects\": [" +
                        "      \"sg-00105f70\"," +
                        "      \"sg-7b135c0b\"," +
                        "      \"sg-b52e61c5\"," +
                        "      \"sg-bc105fcc\"" +
                        "    ]" +
                        "  }," +
                        "  \"ec2-vpc-flow-logs\": {" +
                        "    \"audit_objects\": null," +
                        "    \"formulas\": null," +
                        "    \"link\": \"http://kb.cloudcoreo.com/mydoc_ec2-vpc-flow-logs.html\"," +
                        "    \"description\": \"VPC Flow Logs is a feature that enables you to capture information about the IP traffic going to and from network interfaces in your VPC. After you've created a flow log, you can view and retrieve its data in Amazon CloudWatch Logs.\"," +
                        "    \"call_modifiers\": null," +
                        "    \"report_index\": 16," +
                        "    \"provider\": \"AWS\"," +
                        "    \"operators\": null," +
                        "    \"meta_\": [" +
                        "      {" +
                        "        \"name\": \"meta_cis_id\"," +
                        "        \"value\": {" +
                        "          \"string\": \"4.3\"" +
                        "        }" +
                        "      }," +
                        "      {" +
                        "        \"name\": \"meta_cis_scored\"," +
                        "        \"value\": {" +
                        "          \"string\": \"true\"" +
                        "        }" +
                        "      }," +
                        "      {" +
                        "        \"name\": \"meta_cis_level\"," +
                        "        \"value\": {" +
                        "          \"string\": \"1\"" +
                        "        }" +
                        "      }" +
                        "    ]," +
                        "    \"raise_when\": null," +
                        "    \"id\": \"0e29276a61826f26b268243d3b8add840e29a558\"," +
                        "    \"timestamp\": \"2017-08-30T19:50:25.196Z\"," +
                        "    \"report_size\": 16," +
                        "    \"suggested_action\": \"VPC Flow Logs be enabled for packet 'Rejects' for VPCs.\"," +
                        "    \"id_map\": null," +
                        "    \"level\": \"Low\"," +
                        "    \"display_name\": \"Ensure VPC flow logging is enabled in all VPCs (Scored)\"," +
                        "    \"name_id\": \"ec2-vpc-flow-logs_0e29276\"," +
                        "    \"service\": \"ec2\"," +
                        "    \"name\": \"ec2-vpc-flow-logs\"," +
                        "    \"objectives\": null," +
                        "    \"region\": \"us-east-1\"," +
                        "    \"category\": \"Audit\"," +
                        "    \"include_violations_in_count\": \"true\"," +
                        "    \"violating_objects\": [" +
                        "      \"vpc-4386b13a\"" +
                        "    ]" +
                        "  }," +
                        "  \"s3-logging-disabled\": {" +
                        "    \"audit_objects\": null," +
                        "    \"formulas\": null," +
                        "    \"link\": \"http://kb.cloudcoreo.com/mydoc_s3-logging-disabled.html\"," +
                        "    \"description\": \"S3 bucket logging has not been enabled for the affected resource.\"," +
                        "    \"call_modifiers\": null," +
                        "    \"report_index\": 1," +
                        "    \"provider\": \"AWS\"," +
                        "    \"operators\": null," +
                        "    \"meta_\": [" +
                        "      {" +
                        "        \"name\": \"meta_nist_171_id\"," +
                        "        \"value\": {" +
                        "          \"string\": \"3.1.2\"" +
                        "        }" +
                        "      }" +
                        "    ]," +
                        "    \"raise_when\": null," +
                        "    \"id\": \"1b4dac5ecc468016459b0e3cac5d075bd2b85507\"," +
                        "    \"timestamp\": \"2017-08-30T19:48:52.764Z\"," +
                        "    \"report_size\": 1," +
                        "    \"suggested_action\": \"Enable logging on your S3 buckets.\"," +
                        "    \"id_map\": null," +
                        "    \"level\": \"Low\"," +
                        "    \"display_name\": \"S3 bucket logging not enabled\"," +
                        "    \"name_id\": \"s3-logging-disabled_1b4dac5\"," +
                        "    \"service\": \"s3\"," +
                        "    \"name\": \"s3-logging-disabled\"," +
                        "    \"objectives\": null," +
                        "    \"region\": \"us-east-1\"," +
                        "    \"category\": \"Audit\"," +
                        "    \"include_violations_in_count\": true," +
                        "    \"violating_objects\": [" +
                        "      \"cc-cf-demo\"" +
                        "    ]" +
                        "  }" +
                        "}");
    }

    @Before
    public void setUpJSONObjects() throws Exception {
        jsonObject = getObjectJson();
        jsonArray = getObjectArrayJson();
        emptyJSONObject = JSONObject.fromObject("{}");
    }

    @Test
    public void testArrayObject() {
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            ContextTestResult result = new ContextTestResult(jsonObject);
            Assert.assertEquals("Audit", result.getCategory());
            Assert.assertEquals("ec2", result.getService());
        }
    }

    @Test
    public void testObject() {
        ContextTestResult result = new ContextTestResult(this.jsonObject);
        Assert.assertEquals("Audit", result.getCategory());
        Assert.assertEquals(3, result.getMetaTags().size());
    }
    @Test
    public void testEmpty() {
        ContextTestResult result = new ContextTestResult(this.emptyJSONObject);
        Assert.assertEquals(null, result.getName());
    }


}
