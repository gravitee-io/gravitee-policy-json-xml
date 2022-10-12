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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
public class JsonToXmlTransformationPolicyIntegrationTest
    extends AbstractPolicyTest<JsonToXmlTransformationPolicy, JsonToXmlTransformationPolicyConfiguration> {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "true");
    }

    @Override
    public void configureApi(Api api) {
        api.setExecutionMode(ExecutionMode.JUPITER);
    }

    @Test
    @DisplayName("Should post xml to backend")
    @DeployApi("/apis/api-pre.json")
    void shouldPostXmlContentToBackend(WebClient client) {
        final String input = loadResource("/io/gravitee/policy/json2xml/input.json");
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");

        wiremock.stubFor(post("/team").willReturn(ok()));

        final TestObserver<HttpResponse<Buffer>> obs = client.post("/test").rxSendBuffer(Buffer.buffer(input)).test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return true;
                }
            )
            .assertNoErrors();

        wiremock.verify(1, postRequestedFor(urlPathEqualTo("/team")).withRequestBody(new EqualToPattern(expected)));
    }

    @Test
    @DisplayName("Should return Bad Request when posting invalid json to gateway")
    @DeployApi("/apis/api-pre.json")
    void shouldReturnBadRequestWhenPostingInvalidJsonToGateway(WebClient client) {
        final String input = loadResource("/io/gravitee/policy/json2xml/invalid-input.json");

        final TestObserver<HttpResponse<Buffer>> obs = client.post("/test").rxSendBuffer(Buffer.buffer(input)).test();

        awaitTerminalEvent(obs)
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(400);
                    return true;
                }
            )
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should get xml from gateway")
    @DeployApi("/apis/api-post.json")
    void shouldGetXmlContentFromBackend(WebClient client) {
        final String expected = loadResource("/io/gravitee/policy/json2xml/expected.xml");
        final String backendResponse = loadResource("/io/gravitee/policy/json2xml/input.json");
        wiremock.stubFor(get("/team").willReturn(ok(backendResponse)));

        final TestObserver<HttpResponse<Buffer>> obs = client.get("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.bodyAsString()).isEqualTo(expected);
                    return true;
                }
            )
            .assertNoErrors();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    @DisplayName("Should return Internal Error when getting invalid json from backend")
    @DeployApi("/apis/api-post.json")
    void shouldReturnInternalErrorWhenGettingInvalidXmlContentFromBackend(WebClient client) {
        final String backendResponse = loadResource("/io/gravitee/policy/json2xml/invalid-input.json");
        wiremock.stubFor(get("/team").willReturn(ok(backendResponse)));

        final TestObserver<HttpResponse<Buffer>> obs = client.get("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(500);
                    return true;
                }
            )
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
