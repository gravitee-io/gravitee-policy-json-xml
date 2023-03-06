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
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.gravitee.policy.json2xml.transformer.JSONObject;
import io.gravitee.policy.json2xml.transformer.XML;
import io.gravitee.policy.json2xml.utils.CharsetHelper;
import io.gravitee.policy.v3.json2xml.JsonToXmlTransformationPolicyV3;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JsonToXmlTransformationPolicy extends JsonToXmlTransformationPolicyV3 implements Policy {

    private static final String INVALID_PAYLOAD_FAILURE_KEY = "JSON_INVALID_PAYLOAD";
    private static final String INVALID_MESSAGE_PAYLOAD_FAILURE_KEY = "JSON_INVALID_MESSAGE_PAYLOAD";

    public JsonToXmlTransformationPolicy(final JsonToXmlTransformationPolicyConfiguration configuration) {
        super(configuration);
    }

    private static void setContentHeaders(final HttpHeaders headers, final Buffer xmlBuffer) {
        headers.set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE);
        headers.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(xmlBuffer.length()));
    }

    @Override
    public String id() {
        return "json-xml";
    }

    @Override
    public Completable onRequest(final HttpExecutionContext ctx) {
        return ctx.request().onBody(body -> transformBodyToXml(ctx, body, ctx.request().headers(), HttpStatusCode.BAD_REQUEST_400));
    }

    @Override
    public Completable onResponse(final HttpExecutionContext ctx) {
        return ctx
            .response()
            .onBody(body -> transformBodyToXml(ctx, body, ctx.response().headers(), HttpStatusCode.INTERNAL_SERVER_ERROR_500));
    }

    private Maybe<Buffer> transformBodyToXml(
        final HttpExecutionContext ctx,
        final Maybe<Buffer> bodyUpstream,
        final HttpHeaders httpHeaders,
        final int failureHttpCode
    ) {
        return bodyUpstream
            .flatMap(buffer -> transformToXml(buffer, CharsetHelper.extractCharset(httpHeaders)))
            .doOnSuccess(xmlBuffer -> setContentHeaders(httpHeaders, xmlBuffer))
            .onErrorResumeWith(
                ctx.interruptBodyWith(
                    new ExecutionFailure(failureHttpCode)
                        .key(INVALID_PAYLOAD_FAILURE_KEY)
                        .message("Unable to transform invalid JSON payload to XML")
                )
            );
    }

    @Override
    public Completable onMessageRequest(MessageExecutionContext ctx) {
        return ctx
            .request()
            .onMessage(message -> transformMessageToXml(ctx, message, ctx.request().headers(), HttpStatusCode.BAD_REQUEST_400));
    }

    @Override
    public Completable onMessageResponse(MessageExecutionContext ctx) {
        return ctx
            .response()
            .onMessage(message -> transformMessageToXml(ctx, message, ctx.response().headers(), HttpStatusCode.INTERNAL_SERVER_ERROR_500));
    }

    private Maybe<Message> transformMessageToXml(
        final MessageExecutionContext ctx,
        final Message message,
        final HttpHeaders httpHeaders,
        final int failureHttpCode
    ) {
        return transformToXml(message.content(), CharsetHelper.extractCharset(httpHeaders))
            .map(message::content)
            .doOnSuccess(xmlMessage -> setContentHeaders(message.headers(), xmlMessage.content()))
            .onErrorResumeWith(
                ctx.interruptMessageWith(
                    new ExecutionFailure(failureHttpCode)
                        .key(INVALID_MESSAGE_PAYLOAD_FAILURE_KEY)
                        .message("Unable to transform invalid JSON message to XML")
                )
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
}
