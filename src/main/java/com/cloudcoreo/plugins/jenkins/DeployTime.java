package com.cloudcoreo.plugins.jenkins;

import com.cloudcoreo.plugins.jenkins.exceptions.EndpointUnavailableException;
import com.cloudcoreo.plugins.jenkins.exceptions.ExecutionFailedException;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
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
    public static final Logger log = Logger.getLogger(DeployTime.class.getName());

    private boolean hasContextRunStarted;
    private int domainPort;
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
        return 300;
    }

    boolean contextRunStarted() {
        return hasContextRunStarted;
    }

    private Charset getEncoding() { return Charset.forName("UTF-8"); }

    private String getEndpointURL() {
        return getDomainProtocol() + "://" + getDomain() + ":" + getDomainPort();
    }

    String getDeployTimeURL() {
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
            SecretKeySpec secretKey = new SecretKeySpec(secretAccessKey.getBytes(getEncoding()), hmac);
            sha1HMAC.init(secretKey);
            byte[] bytes = data.getBytes(getEncoding());
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
            byte[] array = md.digest(md5.getBytes(getEncoding()));
            StringBuilder sb = new StringBuilder();
            for (byte byteValue : array) {
                sb.append(Integer.toHexString((byteValue & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | NullPointerException ignored) { }
        return "";
    }

    private String getMediaType(String callType){
        if(callType.equals("POST")){
            return "application/json";
        }
        return "";
    }

    private JSONObject sendSignedRequest(String url, String callType, String body) {
        Gson gson = new Gson();
        body = (body == null) ? "" : body;

        callType = callType.toUpperCase();
        String mediaType = getMediaType(callType);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = dateFormat.format(new Date());

        String message = callType +
                "\n" +
                getMD5Hash(body) +
                "\n" +
                mediaType +
                "\n" +
                date;

        String returnPayload = makeRequest(callType, url, mediaType, message, date, body);
        JSONObject response = gson.fromJson(returnPayload, JSONObject.class);
        if (response != null && response.containsKey("status") && response.getString("status").equals("401")) {
            throw new AccessControlException("Authentication error: user is unauthorized to make API calls.");
        }

        return response;
    }

    private String makeRequest(String callType, String targetUrl, String mediaType, String message, String date, String body) {
        String authHeader = "Hmac " + accessKeyId + ":" + computeHmac1(message);
        HttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()
                ).build();
        HttpResponse response;
        StringEntity stringEntity;
        HttpRequestBase call;

        try {
             stringEntity = new StringEntity(body);

            if (callType.equals("POST")) {
                HttpPost post = (HttpPost) assignHeaders(new HttpPost(targetUrl), mediaType, authHeader, date);
                post.setEntity(stringEntity);
                call = post;
            } else {
                call = assignHeaders(new HttpGet(targetUrl), mediaType, authHeader, date);
            }
            response = makeHttpCall(client, call);

            return parseResponse(response);
        } catch (IOException ignore) {}

        return "";
    }

    private HttpRequestBase assignHeaders(HttpRequestBase request, String mediaType, String authHeader, String date) {
        request.addHeader("Content-Type", mediaType);
        request.addHeader("Authorization", authHeader);
        request.addHeader("Date", date);
        return request;
    }

    String parseResponse(HttpResponse response) {
        InputStreamReader inputReader;
        StringBuilder buffer = new StringBuilder();

        try {
            inputReader = new InputStreamReader(response.getEntity().getContent(), getEncoding());
            BufferedReader reader = new BufferedReader(inputReader);
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            inputReader.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer.toString();
    }

    HttpResponse makeHttpCall(HttpClient client, HttpRequestBase call) throws IOException {
        return client.execute(call);
    }

    void setDeployTimeId(String context, String task) throws EndpointUnavailableException {
        JSONObject body = new JSONObject();
        body.put("context", context);
        body.put("task", task);
        JSONObject deployTimeJsonObject = sendSignedRequest(getDeployTimeURL(), "post", body.toString());
        try {
            deployTimeInstance = new DeployTimeObject(deployTimeJsonObject);
        } catch (NullPointerException e) {
            String message = "\nCloudCoreo server endpoint is currently unavailable, skipping DeployTime analysis\n";
            throw new EndpointUnavailableException(message);
        }
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
        if (!hasContextRunStarted) {
            hasContextRunStarted = job.hasRunningJobs();
        }
        log.info("found jobs?: " + job.hasRunningJobs());
        return job.hasRunningJobs();
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

