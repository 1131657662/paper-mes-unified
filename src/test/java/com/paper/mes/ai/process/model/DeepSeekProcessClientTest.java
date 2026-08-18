package com.paper.mes.ai.process.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.credential.AiProviderCredentialResolver;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeepSeekProcessClientTest {

    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicBoolean nonStreamingResponse = new AtomicBoolean();
    private final AtomicBoolean reasoningOnlyResponse = new AtomicBoolean();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicBoolean holdResponse = new AtomicBoolean();
    private CountDownLatch requestStarted;
    private CountDownLatch releaseResponse;
    private HttpServer server;
    private AiProperties properties;
    private AiProviderCredentialResolver credentialResolver;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus.set(200);
        nonStreamingResponse.set(false);
        reasoningOnlyResponse.set(false);
        authorization.set(null);
        requestBody.set(null);
        holdResponse.set(false);
        requestStarted = new CountDownLatch(1);
        releaseResponse = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", this::respond);
        server.start();
        properties = new AiProperties();
        properties.setProvider("DEEPSEEK");
        properties.setDeepseekApiKey("test-secret");
        properties.setDeepseekBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setDeepseekModelPro("deepseek-v4-pro");
        credentialResolver = mock(AiProviderCredentialResolver.class);
        when(credentialResolver.resolveApiKey(AiProvider.DEEPSEEK))
                .thenReturn(Optional.of("test-secret"));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void parseStreamsContentAndReturnsUsageWithoutExposingProviderChunks() {
        DeepSeekProcessClient client = new DeepSeekProcessClient(
                properties, new ObjectMapper(), credentialResolver);
        List<String> deltas = new ArrayList<>();

        ProcessAiModelResult result = client.parse(
                new ProcessAiModelPrompt("system", "order context"), deltas::add);

        assertThat(deltas).containsExactly("{\"assignments\":", "[]}");
        assertThat(result.content()).isEqualTo("{\"assignments\":[]}");
        assertThat(result.model()).isEqualTo("deepseek-v4-pro-202608");
        assertThat(result.inputTokens()).isEqualTo(120);
        assertThat(result.outputTokens()).isEqualTo(30);
        assertThat(authorization.get()).isEqualTo("Bearer test-secret");
        assertThat(requestBody.get()).contains("\"stream\":true", "\"json_object\"");
        assertThat(requestBody.get()).contains("\"thinking\":{\"type\":\"disabled\"}");
    }

    @Test
    void parseAcceptsCompatibleNonStreamingJsonResponse() {
        nonStreamingResponse.set(true);
        DeepSeekProcessClient client = new DeepSeekProcessClient(
                properties, new ObjectMapper(), credentialResolver);
        List<String> deltas = new ArrayList<>();

        ProcessAiModelResult result = client.parse(
                new ProcessAiModelPrompt("system", "order context"), deltas::add);
        assertThat(result.content()).isEqualTo("{\"assignments\":[]}");
        assertThat(deltas).containsExactly("{\"assignments\":[]}");
    }

    @Test
    void parseRejectsReasoningOnlyResponseAsRetryableEmptyResult() {
        reasoningOnlyResponse.set(true);
        DeepSeekProcessClient client = new DeepSeekProcessClient(
                properties, new ObjectMapper(), credentialResolver);
        List<String> deltas = new ArrayList<>();

        ProcessAiProviderException error = catchThrowableOfType(() -> client.parse(
                new ProcessAiModelPrompt("system", "order context"), deltas::add),
                ProcessAiProviderException.class);
        assertThat(error.failureCode()).isEqualTo("AI_PROVIDER_EMPTY_RESULT");
        assertThat(error.retryable()).isTrue();
        assertThat(deltas).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "401, AI_PROVIDER_AUTH_FAILED, false",
            "402, AI_PROVIDER_PAYMENT_REQUIRED, false",
            "429, AI_PROVIDER_RATE_LIMITED, true",
            "503, AI_PROVIDER_UPSTREAM_ERROR, true"
    })
    void parseClassifiesProviderFailures(int status, String code, boolean retryable) {
        responseStatus.set(status);
        DeepSeekProcessClient client = new DeepSeekProcessClient(
                properties, new ObjectMapper(), credentialResolver);

        ProcessAiProviderException error = catchThrowableOfType(() -> client.parse(
                new ProcessAiModelPrompt("system", "context"), ignored -> {
                }), ProcessAiProviderException.class);
        assertThat(error.failureCode()).isEqualTo(code);
        assertThat(error.retryable()).isEqualTo(retryable);
    }

    @Test
    void parseFailsClosedWhenDeepSeekIsNotConfigured() {
        when(credentialResolver.resolveApiKey(AiProvider.DEEPSEEK)).thenReturn(Optional.empty());
        DeepSeekProcessClient client = new DeepSeekProcessClient(
                properties, new ObjectMapper(), credentialResolver);

        ProcessAiProviderException error = catchThrowableOfType(() -> client.parse(
                new ProcessAiModelPrompt("system", "context"), ignored -> {
                }), ProcessAiProviderException.class);
        assertThat(error.failureCode()).isEqualTo("AI_PROVIDER_NOT_CONFIGURED");
        assertThat(error.retryable()).isFalse();
    }

    @Test
    void parseCancelsProviderCallWhenClientDisconnects() throws Exception {
        holdResponse.set(true);
        DeepSeekProcessClient client = new DeepSeekProcessClient(
                properties, new ObjectMapper(), credentialResolver);
        ProcessAiCancellation cancellation = new ProcessAiCancellation();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> client.parse(
                    new ProcessAiModelPrompt("system", "context"), ignored -> { }, cancellation));
            assertThat(requestStarted.await(2, TimeUnit.SECONDS)).isTrue();

            cancellation.cancel();

            ExecutionException error = catchThrowableOfType(
                    () -> future.get(2, TimeUnit.SECONDS), ExecutionException.class);
            assertThat(error.getCause()).isInstanceOfSatisfying(
                    ProcessAiProviderException.class,
                    failure -> assertThat(failure.failureCode()).isEqualTo("AI_REQUEST_CANCELLED"));
        } finally {
            releaseResponse.countDown();
        }
    }

    @Test
    void springCanWireTheProductionClientConstructor() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProperties.class, () -> properties);
            context.registerBean(ObjectMapper.class);
            context.registerBean(AiProviderCredentialResolver.class, () -> credentialResolver);
            context.register(DeepSeekProcessClient.class);
            context.refresh();

            assertThat(context.getBean(DeepSeekProcessClient.class)).isNotNull();
        }
    }

    private void respond(HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        waitWhenHeld();
        int status = responseStatus.get();
        byte[] body = status == 200
                ? (reasoningOnlyResponse.get()
                ? reasoningOnlyBody()
                : (nonStreamingResponse.get() ? nonStreamingBody() : successBody()))
                : "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type",
                nonStreamingResponse.get() && !reasoningOnlyResponse.get()
                        ? "application/json" : "text/event-stream");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void waitWhenHeld() {
        if (!holdResponse.get()) return;
        requestStarted.countDown();
        try {
            releaseResponse.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private byte[] successBody() {
        String body = """
                data: {"model":"deepseek-v4-pro-202608","choices":[{"delta":{"content":"{\\\"assignments\\\":"}}]}
                data: {"choices":[{"delta":{"content":"[]}"}}]}
                data: {"choices":[],"usage":{"prompt_tokens":120,"completion_tokens":30}}
                data: [DONE]
                """;
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] nonStreamingBody() {
        return "{\"model\":\"deepseek-v4-pro\",\"choices\":[{\"message\":{\"content\":\"{\\\"assignments\\\":[]}\"}}]}"
                .getBytes(StandardCharsets.UTF_8);
    }

    private byte[] reasoningOnlyBody() {
        String body = """
                data: {"model":"deepseek-v4-pro","choices":[{"delta":{"reasoning_content":"internal reasoning"},"finish_reason":"length"}]}
                data: [DONE]
                """;
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
