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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.gravitee.policy.json2xml.transformer.JSONObject;
import io.gravitee.policy.json2xml.transformer.XML;
import io.gravitee.policy.json2xml.utils.CharsetHelper;
import io.gravitee.policy.v3.json2xml.JsonToXmlTransformationPolicyV3;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonToXmlTransformationPolicy extends JsonToXmlTransformationPolicyV3 implements Policy {

    public JsonToXmlTransformationPolicy(final JsonToXmlTransformationPolicyConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String id() {
        return "json-xml";
    }

    @Override
    public Completable onRequest(final HttpExecutionContext ctx) {
        return ctx
            .request()
            .onBody(
                bodyUpstream ->
                    bodyUpstream
                        .flatMap(buffer -> transformToXml(buffer, CharsetHelper.extractCharset(ctx.request().headers())))
                        .doOnSuccess(xmlBuffer -> applyHeaders(ctx.request().headers(), xmlBuffer))
            )
            .onErrorResumeNext(throwable -> interruptWithBadRequest(ctx, throwable));
    }

    @Override
    public Completable onResponse(final HttpExecutionContext ctx) {
        return ctx
            .response()
            .onBody(
                bodyUpstream ->
                    bodyUpstream
                        .flatMap(buffer -> transformToXml(buffer, CharsetHelper.extractCharset(ctx.response().headers())))
                        .doOnSuccess(xmlBuffer -> applyHeaders(ctx.response().headers(), xmlBuffer))
            )
            .onErrorResumeNext(throwable -> interruptWithBadRequest(ctx, throwable));
    }

    @Override
    public Completable onMessageRequest(MessageExecutionContext ctx) {
        return ctx
            .request()
            .onMessage(
                message ->
                    transformToXml(message.content(), CharsetHelper.extractCharset(ctx.request().headers()))
                        .map(message::content)
                        .doOnSuccess(xmlBuffer -> applyHeaders(message.headers(), message.content()))
            );
    }

    @Override
    public Completable onMessageResponse(MessageExecutionContext ctx) {
        return ctx
            .response()
            .onMessage(
                message ->
                    transformToXml(message.content(), CharsetHelper.extractCharset(ctx.response().headers()))
                        .map(message::content)
                        .doOnSuccess(xmlBuffer -> applyHeaders(message.headers(), message.content()))
            );
    }

    private Maybe<Buffer> transformToXml(Buffer buffer, final Charset charset) {
        try {
            String encodedPayload = new String(buffer.toString(charset).getBytes(StandardCharsets.UTF_8));
            JSONObject jsonPayload = new JSONObject(encodedPayload);
            JSONObject jsonPayloadWithRoot = new JSONObject();
            jsonPayloadWithRoot.append(configuration.getRootElement(), jsonPayload);

            return Maybe.just(Buffer.buffer(XML.toString(jsonPayloadWithRoot)));
        } catch (Exception ex) {
            return Maybe.error(new TransformationException("Unable to transform JSON into XML: " + ex.getMessage(), ex));
        }
    }

    private static void applyHeaders(HttpHeaders headers, Buffer xmlBuffer) {
        headers.set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE);
        headers.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(xmlBuffer.length()));
    }

    private static Completable interruptWithBadRequest(HttpExecutionContext ctx, Throwable throwable) {
        return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).message(throwable.getMessage()));
    }
}
