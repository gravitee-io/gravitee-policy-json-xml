package com.graviteesource.policy.json2xml.configuration;

import io.gravitee.policy.api.PolicyConfiguration;

public class JsonToXmlTransformationPolicyConfiguration implements PolicyConfiguration {

    public static final String DEFAULT_ROOT = "root";

    private PolicyScope scope = PolicyScope.RESPONSE;

    private String rootElement = DEFAULT_ROOT;

    public PolicyScope getScope() {
        return scope;
    }

    public void setScope(PolicyScope scope) {
        this.scope = scope;
    }

    public void setRootElement(String rootElement) {
        this.rootElement = rootElement;
    }

    public String getRootElement() {
        return rootElement;
    }
}
