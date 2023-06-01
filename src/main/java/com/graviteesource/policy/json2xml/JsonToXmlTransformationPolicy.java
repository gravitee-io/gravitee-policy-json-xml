package com.graviteesource.policy.json2xml;

import com.graviteesource.policy.deployer.JsonToXmlTransformationPolicyDeploymentLifecycle;
import com.graviteesource.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import com.graviteesource.policy.json2xml.transformer.JSONObject;
import com.graviteesource.policy.json2xml.transformer.JSONTokener;
import com.graviteesource.policy.json2xml.transformer.XML;
import com.graviteesource.policy.json2xml.utils.CharsetHelper;
import com.graviteesource.policy.v3.json2xml.JsonToXmlTransformationPolicyV3;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.api.annotations.Plugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Plugin(deployment = JsonToXmlTransformationPolicyDeploymentLifecycle.class)
public class JsonToXmlTransformationPolicy extends JsonToXmlTransformationPolicyV3 implements Policy {

    public static final String POLICY_JSON_XML_MAXDEPTH = "policy.json-xml.maxdepth";
    public static final String ENVVAR_POLICY_JSON_XML_MAXDEPTH = "gravitee_policy_jsonxml_maxdepth";
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
            .flatMap(buffer -> transformToXml(buffer, CharsetHelper.extractCharset(httpHeaders), getMaxDepth(ctx)))
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
        return transformToXml(
            message.content(),
            CharsetHelper.extractCharset(httpHeaders),
            ctx.getComponent(Configuration.class).getProperty(POLICY_JSON_XML_MAXDEPTH, Integer.class, JSONTokener.DEFAULT_MAX_DEPTH)
        )
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

    private Maybe<Buffer> transformToXml(Buffer buffer, final Charset charset, int maxDepth) {
        try {
            String encodedPayload = new String(buffer.toString(charset).getBytes(StandardCharsets.UTF_8));
            JSONObject jsonPayload = new JSONObject(encodedPayload, maxDepth);
            JSONObject jsonPayloadWithRoot = new JSONObject();
            jsonPayloadWithRoot.append(configuration.getRootElement(), jsonPayload);

            return Maybe.just(Buffer.buffer(XML.toString(jsonPayloadWithRoot)));
        } catch (Exception ex) {
            return Maybe.error(new TransformationException("Unable to transform JSON into XML: " + ex.getMessage(), ex));
        }
    }

    protected int getMaxDepth(GenericExecutionContext ctx) {
        if (this.maxDepth == null) {
            this.maxDepth =
                ctx.getComponent(Configuration.class).getProperty(POLICY_JSON_XML_MAXDEPTH, Integer.class, JSONTokener.DEFAULT_MAX_DEPTH);
        }
        return this.maxDepth;
    }
}
