package com.graviteesource.policy.deployer;

import io.gravitee.node.api.deployer.AbstractPluginDeploymentLifecycle;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonToXmlTransformationPolicyDeploymentLifecycle extends AbstractPluginDeploymentLifecycle {

    private static final String JSON_TO_XML_TRANSFORMATION_POLICY = "apim-policy-json-to-xml";

    @Override
    protected String getFeatureName() {
        return JSON_TO_XML_TRANSFORMATION_POLICY;
    }
}
