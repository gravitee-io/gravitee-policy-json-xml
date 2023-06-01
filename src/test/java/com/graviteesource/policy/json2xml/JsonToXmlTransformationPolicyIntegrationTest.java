package com.graviteesource.policy.json2xml;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.graviteesource.policy.json2xml.configuration.JsonToXmlTransformationPolicyConfiguration;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
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
    void shouldPostXmlContentToBackend(HttpClient client) throws InterruptedException {
        final String input = loadResource("/com/graviteesource/policy/json2xml/input.json");
        final String expected = loadResource("/com/graviteesource/policy/json2xml/expected.xml");

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
        final String input = loadResource("/com/graviteesource/policy/json2xml/invalid-input.json");

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
        final String expected = loadResource("/com/graviteesource/policy/json2xml/expected.xml");
        final String backendResponse = loadResource("/com/graviteesource/policy/json2xml/input.json");
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
        final String backendResponse = loadResource("/com/graviteesource/policy/json2xml/invalid-input.json");
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
