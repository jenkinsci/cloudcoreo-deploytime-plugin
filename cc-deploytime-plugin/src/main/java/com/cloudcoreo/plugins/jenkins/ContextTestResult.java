package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;


final class ContextTestResult implements Serializable {
    private static final long serialVersionUID = 7880238519613578019L;
    private static final Logger log = Logger.getLogger(ContextTestResult.class.getName());

    private String name;
    private String region;
    private String service;
    private String suggestedAction;
    private String timestamp;
    private String category;
    private String description;
    private String displayName;
    private String level;
    private String link;
    private final Map<String, String> metaTags;
    private final List<String> violatingObjects;
    private final JSONObject testResultJson;

    String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    String getRegion() {
        return region;
    }

    String getService() {
        return service;
    }

    String getSuggestedAction() {
        return suggestedAction;
    }

    String getTimestamp() {
        return timestamp;
    }

    String getCategory() {
        return category;
    }

    String getDescription() {
        return description;
    }

    String getDisplayName() {
        return displayName;
    }

    String getLevel() {
        return level;
    }

    String getLink() {
        return link;
    }

    Map<String, String> getMetaTags() {
        return metaTags;
    }

    List<String> getViolatingObjects() {
        return violatingObjects;
    }

    ContextTestResult(JSONObject testResultJson) {
        this.testResultJson = testResultJson;
        metaTags = new HashMap<>();
        violatingObjects = new ArrayList<>();
        parseResults();
    }

    private void parseResults() {
        JSONObject violationObject = null;
        String keyId;
        Iterator iter = testResultJson.keys();
        // first key is a violation id
        while (iter.hasNext()) {
            keyId = (String) iter.next();

            try {
                violationObject = testResultJson.getJSONObject(keyId);
            } catch (JSONException e) {
                // Something went wrong!
            }
        }
        if (violationObject != null) {
            assignInstanceVars(violationObject);

            JSONArray jsonViolatingObjects = violationObject.getJSONArray("violating_objects");
            for (Object jsonViolatingObject : jsonViolatingObjects) {
                violatingObjects.add(jsonViolatingObject.toString());
            }

            JSONArray jsonMetaTags = violationObject.getJSONArray("meta_");
            JSONObject metaTagsObject;
            String metaTagName;
            JSONObject metaTagValueJSON;
            Object metaTagValueObject = null;
            for (int i = 0; i < jsonMetaTags.size(); i++) {
                metaTagsObject = jsonMetaTags.getJSONObject(i);
                metaTagName = metaTagsObject.getString("name");
                metaTagValueJSON = metaTagsObject.getJSONObject("value");

                // now we have to deal with lots of different data types - lets just make them all strings
                Iterator valuesIterator = metaTagValueJSON.keys();
                while (valuesIterator.hasNext()) {
                    keyId = (String) valuesIterator.next();
                    try {
                        metaTagValueObject = metaTagValueJSON.get(keyId);
                    } catch (JSONException e) {
                        log.info(e.getMessage());
                    }
                }
                if (metaTagValueObject != null) {
                    metaTags.put(metaTagName, metaTagValueObject.toString());
                }
            }
        } else {
            assignInstanceVars();
        }
    }

    private void assignInstanceVars(JSONObject violationObject) {
        name = violationObject.getString("name");
        region = violationObject.getString("region");
        service = violationObject.getString("service");
        suggestedAction = violationObject.getString("suggested_action");
        timestamp = violationObject.getString("timestamp");
        category = violationObject.getString("category");
        description = violationObject.getString("description");
        displayName = violationObject.getString("display_name");
        level = violationObject.getString("level");
        link = violationObject.getString("link");
    }

    private void assignInstanceVars() {
        name = null;
        region = null;
        service = null;
        suggestedAction = null;
        timestamp = null;
        category = null;
        description = null;
        displayName = null;
        level = null;
        link = null;
    }
}
