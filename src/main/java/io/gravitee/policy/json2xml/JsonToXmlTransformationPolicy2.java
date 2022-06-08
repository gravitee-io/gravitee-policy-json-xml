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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.gravitee.policy.json2xml.transformer.JSONObject;
import io.gravitee.policy.json2xml.transformer.XML;
import io.gravitee.policy.json2xml.utils.CharsetHelper;
import io.reactivex.Completable;
import io.reactivex.MaybeTransformer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonToXmlTransformationPolicy2 implements Policy {

    private static final String UTF8_CHARSET_NAME = "UTF-8";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_XML + ";charset=" + UTF8_CHARSET_NAME;
    /**
     * Json to xml transformation configuration
     */
    private final JsonToXmlTransformationPolicyConfiguration configuration;

    public JsonToXmlTransformationPolicy2(final JsonToXmlTransformationPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "json-xml";
    }

    @Override
    public Completable onRequest(final RequestExecutionContext ctx) {
        return Completable.defer(
            () -> {
                Charset charset = CharsetHelper.extractCharset(ctx.request().headers());
                ctx.request().headers().set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE);
                return ctx.request().onBody(transformToXml(charset));
            }
        );
    }

    @Override
    public Completable onResponse(final RequestExecutionContext ctx) {
        return Completable.defer(
            () -> {
                Charset charset = CharsetHelper.extractCharset(ctx.response().headers());
                ctx.response().headers().set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE);
                return ctx.response().onBody(transformToXml(charset));
            }
        );
    }

    private MaybeTransformer<Buffer, Buffer> transformToXml(final Charset charset) {
        return bodyUpstream ->
            bodyUpstream.map(
                buffer -> {
                    try {
                        String encodedPayload = new String(buffer.toString(charset).getBytes(StandardCharsets.UTF_8));
                        JSONObject jsonPayload = new JSONObject(encodedPayload);
                        JSONObject jsonPayloadWithRoot = new JSONObject();
                        jsonPayloadWithRoot.append(configuration.getRootElement(), jsonPayload);
                        return Buffer.buffer(XML.toString(jsonPayloadWithRoot));
                    } catch (Exception ex) {
                        throw new TransformationException("Unable to transform JSON into XML: " + ex.getMessage(), ex);
                    }
                }
            );
    }
}
