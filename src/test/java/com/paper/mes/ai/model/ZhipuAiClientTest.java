package com.paper.mes.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ZhipuAiClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void rewriteSendsApprovedRuleDataAndParsesStructuredResponse() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        startServer(exchange -> {
            captured.set(capture(exchange));
            respond(exchange, 200, successResponse("请按状态规则检查。", "E001-STATUS-GUARD"));
        });

        Optional<AiModelResult> result = client(500).rewrite(prompt());

        assertThat(result).contains(new AiModelResult("请按状态规则检查。", List.of("E001-STATUS-GUARD")));
        assertRequestContract(captured.get());
    }

    @Test
    void rewriteReturnsEmptyWhenProviderRateLimitsRequest() throws Exception {
        startServer(exchange -> respond(exchange, 429, "{\"error\":{\"code\":\"rate_limit\"}}"));

        Optional<AiModelResult> result = client(500).rewrite(prompt());

        assertThat(result).isEmpty();
    }

    @Test
    void rewriteReturnsEmptyWhenProviderTimesOut() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(250);
                respond(exchange, 200, successResponse("迟到响应", "E001-STATUS-GUARD"));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // The client closes the local mock connection after its read timeout.
            }
        });

        Optional<AiModelResult> result = client(100).rewrite(prompt());

        assertThat(result).isEmpty();
    }

    @Test
    void rewriteReturnsEmptyWhenProviderOutputIsNotStructuredJson() throws Exception {
        startServer(exchange -> respond(exchange, 200, successResponseContent("普通文本回答")));

        Optional<AiModelResult> result = client(500).rewrite(prompt());

        assertThat(result).isEmpty();
    }

    private void assertRequestContract(CapturedRequest request) throws Exception {
        assertThat(request.path()).isEqualTo("/api/paas/v4/chat/completions");
        assertThat(request.authorization()).isEqualTo("Bearer test-key");
        JsonNode body = MAPPER.readTree(request.body());
        assertThat(body.path("model").asText()).isEqualTo("glm-4.7-flash");
        assertThat(body.path("max_tokens").asInt()).isEqualTo(1_000);
        assertThat(body.path("messages").path(0).path("content").asText())
                .contains("{\"answer\":string,\"citationRuleIds\":string[]}")
                .doesNotContain("\\\"answer\\\"");
        assertThat(request.body()).contains("E001-STATUS-GUARD", "确定性规则结论")
                .doesNotContain("question", "contextRef", "用户原始问题");
    }

    private ZhipuAiClient client(int readTimeoutMs) {
        AiProperties properties = new AiProperties();
        properties.setProvider(AiProvider.ZHIPU.name());
        properties.setZhipuApiKey("test-key");
        properties.setZhipuBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setZhipuConnectTimeoutMs(500);
        properties.setZhipuReadTimeoutMs(readTimeoutMs);
        return new ZhipuAiClient(properties, MAPPER);
    }

    private AiModelPrompt prompt() {
        return new AiModelPrompt("process-order-detail", List.of("E001-STATUS-GUARD"),
                "状态限制", "确定性规则结论", List.of("查看当前状态"));
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/paas/v4/chat/completions", exchange -> handler.handle(exchange));
        server.start();
    }

    private CapturedRequest capture(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new CapturedRequest(exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"), body);
    }

    private String successResponse(String answer, String ruleId) throws IOException {
        return successResponseContent(MAPPER.writeValueAsString(new AiModelResult(answer, List.of(ruleId))));
    }

    private String successResponseContent(String content) throws IOException {
        return MAPPER.writeValueAsString(new ProviderResponse(List.of(
                new Choice(new Message(content)))));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record CapturedRequest(String path, String authorization, String body) {
    }

    private record ProviderResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
