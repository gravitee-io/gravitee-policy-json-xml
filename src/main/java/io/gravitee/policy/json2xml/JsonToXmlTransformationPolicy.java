/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.json2xml;

import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.http.stream.TransformableResponseStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.gravitee.policy.json2xml.configuration.PolicyScope;
import io.gravitee.policy.json2xml.transformer.JSONObject;
import io.gravitee.policy.json2xml.transformer.JSONTokener;
import io.gravitee.policy.json2xml.transformer.XML;
import io.gravitee.policy.json2xml.utils.CharsetHelper;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonToXmlTransformationPolicy {

    public static final String POLICY_JSON_XML_MAXDEPTH = "policy.json-xml.maxdepth";
    private static final String UTF8_CHARSET_NAME = "UTF-8";
    static final String CONTENT_TYPE = MediaType.APPLICATION_XML + ";charset=" + UTF8_CHARSET_NAME;

    /**
     * Json to xml transformation configuration
     */
    private final JsonToXmlTransformationPolicyConfiguration configuration;

    private Integer maxDepth;

    public JsonToXmlTransformationPolicy(final JsonToXmlTransformationPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Response response, PolicyChain chain, ExecutionContext ctx) {
        if (configuration.getScope() == null || configuration.getScope() == PolicyScope.RESPONSE) {
            Charset charset = CharsetHelper.extractCharset(response.headers());

            return TransformableResponseStreamBuilder
                .on(response)
                .chain(chain)
                .contentType(CONTENT_TYPE)
                .transform(map(charset, getMaxDepth(ctx)))
                .build();
        }
        return null;
    }

    @OnRequestContent
    public ReadWriteStream onRequestContent(Request request, PolicyChain chain, ExecutionContext ctx) {
        if (configuration.getScope() == PolicyScope.REQUEST) {
            Charset charset = CharsetHelper.extractCharset(request.headers());

            return TransformableRequestStreamBuilder
                .on(request)
                .chain(chain)
                .contentType(CONTENT_TYPE)
                .transform(map(charset, getMaxDepth(ctx)))
                .build();
        }
        return null;
    }

    private Function<Buffer, Buffer> map(Charset charset, int maxDepth) {
        return input -> {
            try {
                String encodedPayload = new String(input.toString(charset).getBytes(UTF8_CHARSET_NAME));
                JSONObject jsonPayload = new JSONObject(encodedPayload, maxDepth);
                JSONObject jsonPayloadWithRoot = new JSONObject();
                jsonPayloadWithRoot.append(this.configuration.getRootElement(), jsonPayload);
                return Buffer.buffer(XML.toString(jsonPayloadWithRoot), UTF8_CHARSET_NAME);
            } catch (Exception ex) {
                throw new TransformationException("Unable to transform JSON into XML: " + ex.getMessage(), ex);
            }
        };
    }

    protected int getMaxDepth(ExecutionContext ctx) {
        if (this.maxDepth == null) {
            this.maxDepth =
                ctx.getComponent(Configuration.class).getProperty(POLICY_JSON_XML_MAXDEPTH, Integer.class, JSONTokener.DEFAULT_MAX_DEPTH);
        }
        return this.maxDepth;
    }
}
