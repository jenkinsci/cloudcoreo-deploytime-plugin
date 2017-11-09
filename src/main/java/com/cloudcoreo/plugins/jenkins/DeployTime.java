package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.ExecutionFailedException;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Client;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;


/**
 * Created by paul.allen on 8/10/17.
 */
public class DeployTime implements Serializable {

    private static final long serialVersionUID = -6742613356269048443L;
    private static int TIMEOUT_LIMIT = 300;
    public static final Logger log = Logger.getLogger(DeployTime.class.getName());

    private boolean hasContextRunStarted;
    private int domainPort;
    private final Charset encoding = Charset.forName("UTF-8");
    private Date contextStartTimestamp;
    private Date contextStopTimestamp;
    private String domain;
    private String teamId;
    private String domainProtocol;
    private String accessKeyId;
    private String secretAccessKey;
    private DeployTimeObject deployTimeInstance;

    DeployTime(String teamId, String domain, int domainPort, String domainProtocol, String accessKeyId, String secretAccessKey) {
        this.domain = domain;
        this.domainPort = domainPort;
        this.domainProtocol = domainProtocol;
        this.teamId = teamId;
        hasContextRunStarted = false;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    @SuppressWarnings("WeakerAccess")
    public String getDomain() {
        return domain;
    }

    @SuppressWarnings("WeakerAccess")
    public String getDomainProtocol() {
        return domainProtocol;
    }

    @SuppressWarnings("WeakerAccess")
    public int getDomainPort() {
        return domainPort;
    }

    int getTimeoutLimit() {
        return TIMEOUT_LIMIT;
    }

    private String getEndpointURL() {
        return getDomainProtocol() + "://" + getDomain() + ":" + getDomainPort();
    }

    private String getDeployTimeURL() {
        return getEndpointURL() + "/api/teams/" + teamId + "/devtime";
    }

    private String getContextToggleURL() {
        return getEndpointURL() + "/api/devtime/" + getDeployTimeInstance().getId() + "/";
    }

    DeployTimeObject getDeployTimeInstance() {
        return deployTimeInstance;
    }

    private String computeHmac1(String data) {
        String hmac = "HmacSHA1";
        try {
            Mac sha1HMAC = Mac.getInstance(hmac);
            SecretKeySpec secretKey = new SecretKeySpec(secretAccessKey.getBytes(encoding), hmac);
            sha1HMAC.init(secretKey);
            byte[] bytes = data.getBytes(encoding);
            return Base64.encodeBase64String(sha1HMAC.doFinal(bytes));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.info(e.getMessage());
            log.info(Arrays.toString(e.getStackTrace()));
            return "";
        }
    }

    private String getMD5Hash(String md5) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes(encoding));
            StringBuilder sb = new StringBuilder();
            for (byte byteValue : array) {
                sb.append(Integer.toHexString((byteValue & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ignored) { }
        return "";
    }

    private String getMediaType(String callType){
        if(callType.equals("POST")){
            return MediaType.APPLICATION_JSON;
        }
        return "";
    }

    JSONObject sendSignedRequest(String url, String callType, String body) {

        Gson gson = new Gson();

        Client client = ClientBuilder.newClient();

        callType = callType.toUpperCase();
        String mediaType = getMediaType(callType);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = dateFormat.format(new Date());
        String bodyToHash = (body == null) ? "" : body;

        String message = callType +
                "\n" +
                getMD5Hash(bodyToHash) +
                "\n" +
                mediaType +
                "\n" +
                date;

        String returnPayload = makeRequest(callType, client.target(url), mediaType, message, date, body);
        JSONObject response = gson.fromJson(returnPayload, JSONObject.class);
        if (response != null && response.containsKey("status") && response.getString("status").equals("401")) {
            throw new AccessControlException("Authentication error: user is unauthorized to make API calls.");
        }

        return response;
    }

    private String makeRequest(String callType, WebTarget target, String mediaType, String message, String date, String body) {
        String authHeader = "Hmac " + accessKeyId + ":" + computeHmac1(message);

        if (callType.equals("POST")) {
            return target.request(mediaType)
                    .header("Authorization", authHeader)
                    .header("Date", date)
                    .post(Entity.entity(body, mediaType))
                    .readEntity(String.class);
        } else if (callType.equals("GET")) {
            return target.request()
                    .header("Authorization", authHeader)
                    .header("Date", date)
                    .get()
                    .readEntity(String.class);
        }
        return "";
    }


    void setDeployTimeId(String context, String task) {
        JSONObject body = new JSONObject();
        body.put("context", context);
        body.put("task", task);
        JSONObject deployTimeJsonObject = sendSignedRequest(getDeployTimeURL(), "post", body.toString());
        deployTimeInstance = new DeployTimeObject(deployTimeJsonObject);
        log.info("context set");
    }

    void sendStartContext() {
        hasContextRunStarted = false;
        contextStartTimestamp = new Date();
        String startEndpoint = getContextToggleURL() + "start";
        sendSignedRequest(startEndpoint, "get", null);
    }

    void sendStopContext() {
        contextStopTimestamp = new Date();
        String stopEndpoint = getContextToggleURL() + "stop";
        sendSignedRequest(stopEndpoint, "get", null);
    }

    boolean hasRunningJobs() throws ExecutionFailedException {
        Link deployTimeStatus = getDeployTimeInstance().getStatus();
        JSONObject returnObject = sendSignedRequest(deployTimeStatus.getHref(), deployTimeStatus.getMethod(), null);
        ContextRun job = new ContextRun(returnObject.getJSONObject("status"));
        if (job.hasExecutionFailed()) {
            String message = "CloudCoreo DeployTime job has an error or been manually terminated";
            throw new ExecutionFailedException(message);
        }
        hasContextRunStarted = job.hasRunningJobs();
        log.info("found jobs?: " + job.hasRunningJobs());
        return hasContextRunStarted;
    }

    boolean contextRunTimedOut() {
        if (contextStopTimestamp == null || contextStartTimestamp == null) {
            return false;
        }
        long currentTime = new Date().getTime();
        long diffSeconds = (currentTime - contextStopTimestamp.getTime()) / 1000;
        log.info("been waiting for " + diffSeconds + " seconds");
        return diffSeconds > getTimeoutLimit() || hasContextRunStarted;
    }

    List<ContextTestResult> getResults() {
        ContextTestResult contextResult;
        JSONObject discoveredObject;

        List<ContextTestResult> allResults = new ArrayList<>();
        Link deployTimeResults = getDeployTimeInstance().getResults();
        JSONObject resultJSONObject = sendSignedRequest(deployTimeResults.getHref(), deployTimeResults.getMethod(), null);
        Object resultTest = resultJSONObject.get("results");

        if (resultTest instanceof JSONArray) {
            // It's an array
            JSONArray resultJSONArray = (JSONArray) resultTest;
            for (int i = 0; i < resultJSONArray.size(); i++) {
                discoveredObject = resultJSONArray.getJSONObject(i);
                contextResult = new ContextTestResult(discoveredObject);
                if (contextResult.getName() != null) {
                    allResults.add(contextResult);
                }
            }
        } else {
            // It's an object
            resultJSONObject = (JSONObject) resultTest;
            Object[] resultKeys = resultJSONObject.keySet().toArray();
            for (Object resultKey : resultKeys) {
                String resultKeyString = (String) resultKey;
                discoveredObject = new JSONObject();
                discoveredObject.put(resultKeyString, resultJSONObject.get(resultKeyString));
                contextResult = new ContextTestResult(discoveredObject);

                if (contextResult.getName() != null) {
                    allResults.add(contextResult);
                }
            }
        }
        return allResults;
    }

}

