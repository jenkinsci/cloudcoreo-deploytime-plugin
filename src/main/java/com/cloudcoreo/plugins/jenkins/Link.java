package com.cloudcoreo.plugins.jenkins;

import net.sf.json.JSONObject;

import java.io.Serializable;

class Link implements Serializable {
    private static final long serialVersionUID = -2683982652420880270L;

    private final String href, ref, method;

    String getHref() {
        return href;
    }

    String getRef() {
        return ref;
    }

    String getMethod() {
        return method;
    }

    Link(JSONObject link){
        href = link.getString("href");
        ref = link.getString("ref");
        method = link.getString("method");
    }

}
