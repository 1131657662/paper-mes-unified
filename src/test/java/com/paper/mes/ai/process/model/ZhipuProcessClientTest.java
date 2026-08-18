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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZhipuProcessClientTest {

    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private AiProperties properties;
    private AiProviderCredentialResolver credentials;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/paas/v4/chat/completions", this::respond);
        server.start();
        properties = new AiProperties();
        properties.setZhipuBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setZhipuModel("glm-4.7-flash");
        credentials = mock(AiProviderCredentialResolver.class);
        when(credentials.resolveApiKey(AiProvider.ZHIPU)).thenReturn(Optional.of("glm-secret"));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void parseStreamsGlmJsonAndReportsFallbackRoute() {
        ZhipuProcessClient client = new ZhipuProcessClient(
                properties, new ObjectMapper(), credentials);
        List<String> deltas = new ArrayList<>();

        ProcessAiModelResult result = client.parse(
                new ProcessAiModelPrompt("system", "context"), deltas::add);

        assertThat(deltas).containsExactly("{\"assignments\":", "[]}");
        assertThat(result.content()).isEqualTo("{\"assignments\":[]}");
        assertThat(result.provider()).isEqualTo("ZHIPU");
        assertThat(result.route()).isEqualTo("FALLBACK");
        assertThat(authorization.get()).isEqualTo("Bearer glm-secret");
        assertThat(requestBody.get()).contains("\"model\":\"glm-4.7-flash\"", "\"stream\":true");
    }

    @Test
    void springCanWireTheProductionClientConstructor() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProperties.class, () -> properties);
            context.registerBean(ObjectMapper.class);
            context.registerBean(AiProviderCredentialResolver.class, () -> credentials);
            context.register(ZhipuProcessClient.class);
            context.refresh();

            assertThat(context.getBean(ZhipuProcessClient.class)).isNotNull();
        }
    }

    private void respond(HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] body = """
                data: {"model":"glm-4.7-flash","choices":[{"delta":{"content":"{\\"assignments\\":"}}]}
                data: {"choices":[{"delta":{"content":"[]}"}}]}
                data: {"choices":[],"usage":{"prompt_tokens":10,"completion_tokens":5}}
                data: [DONE]
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
