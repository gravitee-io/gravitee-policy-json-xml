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

import static io.gravitee.policy.v3.json2xml.JsonToXmlTransformationPolicyV3.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class JsonToXmlTransformationPolicyTest {

    private JsonToXmlTransformationPolicy cut;

    @Mock
    private JsonToXmlTransformationPolicyConfiguration configuration;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Captor
    private ArgumentCaptor<MaybeTransformer<Buffer, Buffer>> onBodyCaptor;

    @Captor
    private ArgumentCaptor<Function<Message, Maybe<Message>>> onMessageCaptor;

    @BeforeEach
    public void setUp() {
        cut = new JsonToXmlTransformationPolicy(configuration);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);

        lenient().when(request.headers()).thenReturn(HttpHeaders.create());
        lenient().when(response.headers()).thenReturn(HttpHeaders.create());
    }

    @Test
    @DisplayName("Should transform and add header OnRequest")
    public void shouldTransformAndAddHeadersOnRequest() throws Exception {
        final String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");
        final HttpHeaders headers = HttpHeaders.create();

        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        when(configuration.getRootElement()).thenReturn("root");
        when(request.headers()).thenReturn(headers);

        final TestObserver<Void> obs = cut.onRequest(ctx).test();
        obs.assertNoValues();

        final TestObserver<Buffer> bodyObs = ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer(input)))).test();

        bodyObs.assertValue(buffer -> expected.equals(buffer.toString()));
        verifyHeaders(headers);
    }

    @Test
    @DisplayName("Should do nothing when no body OnRequest")
    public void shouldDoNothingWhenNoBodyOnRequest() {
        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.onRequest(ctx).test();
        obs.assertNoValues();

        final TestObserver<Buffer> bodyObs = ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.empty())).test();

        bodyObs.assertNoValues();
        verify(request, never()).headers();
    }

    @Test
    @DisplayName("Should interrupt with failure when bad json OnRequest")
    public void shouldInterruptWhenBadJsonOnRequest() {
        when(request.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        when(request.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.onRequest(ctx).test();
        obs.assertNoValues();

        final TestObserver<Buffer> bodyObs = ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer("Bad Json")))).test();

        bodyObs.assertError(TransformationException.class);
    }

    @Test
    @DisplayName("Should transform and add header OnResponse")
    public void shouldTransformAndAddHeadersOnResponse() throws Exception {
        final String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");
        final HttpHeaders headers = HttpHeaders.create();

        when(response.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        when(configuration.getRootElement()).thenReturn("root");
        when(response.headers()).thenReturn(headers);

        final TestObserver<Void> obs = cut.onResponse(ctx).test();
        obs.assertNoValues();

        final TestObserver<Buffer> bodyObs = ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer(input)))).test();

        bodyObs.assertValue(buffer -> expected.equals(buffer.toString()));
        verifyHeaders(headers);
    }

    @Test
    @DisplayName("Should do nothing when no body OnResponse")
    public void shouldDoNothingWhenNoBodyOnResponse() throws Exception {
        when(response.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.onResponse(ctx).test();
        obs.assertNoValues();

        final TestObserver<Buffer> bodyObs = ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.empty())).test();

        bodyObs.assertNoValues();
        verify(response, never()).headers();
    }

    @Test
    @DisplayName("Should interrupt with failure when bad json OnResponse")
    public void shouldInterruptWhenBadJsonOnResponse() {
        when(response.onBody(onBodyCaptor.capture())).thenReturn(Completable.complete());
        when(response.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.onResponse(ctx).test();
        obs.assertNoValues();

        final TestObserver<Buffer> bodyObs = ((Maybe<Buffer>) onBodyCaptor.getValue().apply(Maybe.just(Buffer.buffer("Bad Json")))).test();

        bodyObs.assertError(TransformationException.class);
    }

    @Test
    @DisplayName("Should transform OnMessageRequest")
    public void shouldTransformOnMessageRequest() throws Exception {
        final String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");

        when(request.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        when(configuration.getRootElement()).thenReturn("root");
        when(request.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.onMessageRequest(ctx).test();
        obs.assertNoValues();

        final TestObserver<Message> bodyObs = onMessageCaptor.getValue().apply(new DefaultMessage(input)).test();

        bodyObs.assertValue(
            message -> {
                assertThat(expected.equals(message.content().toString())).isTrue();
                verifyHeaders(message.headers());
                return true;
            }
        );
    }

    @Test
    @DisplayName("Should transform OnMessageResponse")
    public void shouldTransformOnMessageResponse() throws Exception {
        final String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");

        when(response.onMessage(onMessageCaptor.capture())).thenReturn(Completable.complete());
        when(configuration.getRootElement()).thenReturn("root");
        when(response.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.onMessageResponse(ctx).test();
        obs.assertNoValues();

        final TestObserver<Message> bodyObs = onMessageCaptor.getValue().apply(new DefaultMessage(input)).test();

        bodyObs.assertValue(
            message -> {
                assertThat(expected.equals(message.content().toString())).isTrue();
                verifyHeaders(message.headers());
                return true;
            }
        );
    }

    private void verifyHeaders(HttpHeaders headers) {
        assertThat(headers.names()).contains(HttpHeaderNames.CONTENT_TYPE);
        assertThat(headers.getAll(HttpHeaderNames.CONTENT_TYPE).get(0)).isEqualTo(CONTENT_TYPE);
        assertThat(headers.names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(headers.names()).contains(HttpHeaderNames.CONTENT_LENGTH);
    }

    private String loadResource(String resource) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(resource)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
