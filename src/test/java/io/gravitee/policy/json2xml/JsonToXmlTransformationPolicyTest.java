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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.gravitee.policy.json2xml.configuration.PolicyScope;
import io.gravitee.reporter.api.http.Metrics;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class JsonToXmlTransformationPolicyTest {

    private JsonToXmlTransformationPolicy cut;

    @Mock
    private JsonToXmlTransformationPolicyConfiguration configuration;

    @Mock
    private PolicyChain policyChain;

    @Spy
    private Request request;

    @Spy
    private Response response;

    @BeforeEach
    public void setUp() {
        cut = new JsonToXmlTransformationPolicy(configuration);
    }

    @Test
    @DisplayName("Should transform and add header OnRequestContent")
    public void shouldTransformAndAddHeadersOnRequestContent() throws Exception {
        String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");

        // Prepare context
        when(configuration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(configuration.getRootElement()).thenReturn("root");
        when(request.headers()).thenReturn(HttpHeaders.create());

        final ReadWriteStream result = cut.onRequestContent(request, policyChain);
        assertThat(result).isNotNull();
        result.bodyHandler(
            resultBody -> {
                assertResultingJsonObjectsAreEquals(expected, resultBody);
            }
        );

        result.write(Buffer.buffer(input));
        result.end();

        assertThat(request.headers().names()).contains(HttpHeaderNames.CONTENT_TYPE);
        assertThat(request.headers().getAll(HttpHeaderNames.CONTENT_TYPE).get(0)).isEqualTo(JsonToXmlTransformationPolicy.CONTENT_TYPE);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(request.headers().names()).contains(HttpHeaderNames.CONTENT_LENGTH);
    }

    @Test
    @DisplayName("Should not transform when TransformationException thrown OnRequestContent")
    public void shouldNotTransformAndAddHeadersOnRequestContent() throws Exception {
        String input = loadResource("/io/gravitee/policy/json2xml/invalid-input.json");

        // Prepare context
        when(configuration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(request.metrics()).thenReturn(Metrics.on(Instant.now().toEpochMilli()).build());

        final ReadWriteStream result = cut.onRequestContent(request, policyChain);
        assertThat(result).isNotNull();

        result.write(Buffer.buffer(input));
        result.end();

        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_TYPE);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_LENGTH);
        assertThat(request.metrics().getMessage()).contains("Unable to transform JSON into XML:");
        verify(policyChain, times(1)).streamFailWith(any());
    }

    @Test
    @DisplayName("Should throw TransformationException if maxDepth reach for object")
    public void shouldLimitObjectDepthOnRequestContent() throws Exception {
        String input = loadResource("/io/gravitee/policy/json2xml/invalid-embedded-object.json");

        // Prepare context
        when(configuration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(request.metrics()).thenReturn(Metrics.on(Instant.now().toEpochMilli()).build());

        final ReadWriteStream result = cut.onRequestContent(request, policyChain);
        assertThat(result).isNotNull();

        result.write(Buffer.buffer(input));
        result.end();

        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_TYPE);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_LENGTH);
        assertThat(request.metrics().getMessage()).contains("Unable to transform JSON into XML:");
        assertThat(request.metrics().getMessage()).contains("Too many nested objects or arrays");
        verify(policyChain, times(1)).streamFailWith(any());
    }

    @Test
    @DisplayName("Should throw TransformationException if maxDepth reach for array")
    public void shouldLimitArrayDepthOnRequestContent() throws Exception {
        String input = loadResource("/io/gravitee/policy/json2xml/invalid-embedded-array.json");

        // Prepare context
        when(configuration.getScope()).thenReturn(PolicyScope.REQUEST);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(request.metrics()).thenReturn(Metrics.on(Instant.now().toEpochMilli()).build());

        final ReadWriteStream result = cut.onRequestContent(request, policyChain);
        assertThat(result).isNotNull();

        result.write(Buffer.buffer(input));
        result.end();

        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_TYPE);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(request.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_LENGTH);
        assertThat(request.metrics().getMessage()).contains("Unable to transform JSON into XML:");
        assertThat(request.metrics().getMessage()).contains("Too many nested objects or arrays");
        verify(policyChain, times(1)).streamFailWith(any());
    }

    @Test
    @DisplayName("Should transform and add header OnResponseContent")
    public void shouldTransformAndAddHeadersOnResponseContent() throws Exception {
        String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");

        // Prepare context
        when(configuration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(configuration.getRootElement()).thenReturn("root");
        when(response.headers()).thenReturn(HttpHeaders.create());

        final ReadWriteStream result = cut.onResponseContent(response, policyChain);
        assertThat(result).isNotNull();
        result.bodyHandler(
            resultBody -> {
                assertResultingJsonObjectsAreEquals(expected, resultBody);
            }
        );

        result.write(Buffer.buffer(input));
        result.end();

        assertThat(response.headers().names()).contains(HttpHeaderNames.CONTENT_TYPE);
        assertThat(response.headers().getAll(HttpHeaderNames.CONTENT_TYPE).get(0)).isEqualTo(JsonToXmlTransformationPolicy.CONTENT_TYPE);
        assertThat(response.headers().names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(response.headers().names()).contains(HttpHeaderNames.CONTENT_LENGTH);
    }

    @Test
    @DisplayName("Should not transform when TransformationException thrown OnResponseContent")
    public void shouldNotTransformAndAddHeadersOnResponseContent() throws Exception {
        String input = loadResource("/io/gravitee/policy/json2xml/invalid-input.json");

        // Prepare context
        when(configuration.getScope()).thenReturn(PolicyScope.RESPONSE);
        when(response.headers()).thenReturn(HttpHeaders.create());

        final ReadWriteStream result = cut.onResponseContent(response, policyChain);
        assertThat(result).isNotNull();

        result.write(Buffer.buffer(input));
        result.end();

        assertThat(response.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_TYPE);
        assertThat(response.headers().names()).doesNotContain(HttpHeaderNames.TRANSFER_ENCODING);
        assertThat(response.headers().names()).doesNotContain(HttpHeaderNames.CONTENT_LENGTH);
        verify(policyChain, times(1)).streamFailWith(any());
    }

    private void assertResultingJsonObjectsAreEquals(String expected, Object resultBody) {
        assertThat(resultBody.toString()).isEqualTo(expected);
    }

    private String loadResource(String resource) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resource);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
