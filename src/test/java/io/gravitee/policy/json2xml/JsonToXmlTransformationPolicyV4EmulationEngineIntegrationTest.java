/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.json2xml;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
public class JsonToXmlTransformationPolicyV4EmulationEngineIntegrationTest
    extends AbstractPolicyTest<JsonToXmlTransformationPolicy, JsonToXmlTransformationPolicyConfiguration> {

    @Test
    @DisplayName("Should post xml to backend")
    @DeployApi("/apis/api-pre.json")
    void shouldPostXmlContentToBackend(HttpClient client) throws InterruptedException {
        final String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");

        wiremock.stubFor(post("/team").willReturn(ok()));

        client
            .rxRequest(POST, "/test")
            .flatMap(request -> request.rxSend(Buffer.buffer(input)))
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(1, postRequestedFor(urlPathEqualTo("/team")).withRequestBody(new EqualToPattern(expected)));
    }

    @Test
    @DisplayName("Should return Bad Request when posting invalid json to gateway")
    @DeployApi("/apis/api-pre.json")
    void shouldReturnBadRequestWhenPostingInvalidJsonToGateway(HttpClient client) throws InterruptedException {
        final String input = loadResource("/io/gravitee/policy/json2xml/invalid-input.json");

        client
            .rxRequest(POST, "/test")
            .flatMap(request -> request.rxSend(Buffer.buffer(input)))
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(400);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should get xml from gateway")
    @DeployApi("/apis/api-post.json")
    void shouldGetXmlContentFromBackend(HttpClient client) throws InterruptedException {
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");
        final String backendResponse = loadResource("/io/gravitee/policy/json2xml/input.json");
        wiremock.stubFor(get("/team").willReturn(ok(backendResponse)));

        client
            .rxRequest(GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DisplayName("Should return Internal Error when getting invalid json from backend")
    @DeployApi("/apis/api-post.json")
    void shouldReturnInternalErrorWhenGettingInvalidXmlContentFromBackend(HttpClient client) throws InterruptedException {
        final String backendResponse = loadResource("/io/gravitee/policy/json2xml/invalid-input.json");
        wiremock.stubFor(get("/team").willReturn(ok(backendResponse)));

        client
            .rxRequest(GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(500);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    protected String loadResource(String resource) {
        try (InputStream is = this.getClass().getResourceAsStream(resource)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
