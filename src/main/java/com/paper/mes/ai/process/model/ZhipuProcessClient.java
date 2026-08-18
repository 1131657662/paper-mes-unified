package com.paper.mes.ai.process.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.credential.AiProviderCredentialResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** GLM process parser used only as the DeepSeek fallback. */
@Component
public class ZhipuProcessClient implements ProcessAiModelClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final AiProviderCredentialResolver credentialResolver;
    private final HttpClient httpClient;

    @Autowired
    public ZhipuProcessClient(AiProperties properties, ObjectMapper objectMapper,
                              AiProviderCredentialResolver credentialResolver) {
        this(properties, objectMapper, credentialResolver, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getZhipuConnectTimeoutMs())).build());
    }

    ZhipuProcessClient(AiProperties properties, ObjectMapper objectMapper,
                       AiProviderCredentialResolver credentialResolver, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.credentialResolver = credentialResolver;
        this.httpClient = httpClient;
    }

    @Override
    public ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer) {
        return parse(prompt, deltaConsumer, new ProcessAiCancellation());
    }

    @Override
    public ProcessAiModelResult parse(ProcessAiModelPrompt prompt, Consumer<String> deltaConsumer,
                                      ProcessAiCancellation cancellation) {
        Thread requestThread = Thread.currentThread();
        try (ProcessAiCancellation.Registration ignored = cancellation.onCancel(requestThread::interrupt)) {
            cancellation.throwIfCancelled();
            HttpResponse<Stream<String>> response = httpClient.send(request(prompt, apiKey()),
                    HttpResponse.BodyHandlers.ofLines());
            cancellation.throwIfCancelled();
            requireSuccess(response.statusCode());
            return consume(response.body(), deltaConsumer, cancellation);
        } catch (ProcessAiProviderException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            if (cancellation.isCancelled()) throw cancelled();
            Thread.currentThread().interrupt();
            throw failure("AI_PROVIDER_INTERRUPTED", true, "GLM provider call was interrupted");
        } catch (Exception exception) {
            if (cancellation.isCancelled()) throw cancelled();
            throw failure("AI_PROVIDER_UNAVAILABLE", true, "GLM provider is unavailable");
        }
    }

    private HttpRequest request(ProcessAiModelPrompt prompt, String apiKey) throws Exception {
        String baseUrl = properties.getZhipuBaseUrl().replaceAll("/+$", "");
        return HttpRequest.newBuilder(URI.create(baseUrl + "/api/paas/v4/chat/completions"))
                .timeout(Duration.ofMillis(properties.getZhipuReadTimeoutMs()))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body(prompt))).build();
    }

    private String body(ProcessAiModelPrompt prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", properties.getZhipuModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", prompt.systemInstruction()),
                        Map.of("role", "user", "content", prompt.userContext())),
                "temperature", 0,
                "max_tokens", properties.getZhipuMaxOutputTokens(),
                "response_format", Map.of("type", "json_object"),
                "stream", true);
        return objectMapper.writeValueAsString(body);
    }

    private ProcessAiModelResult consume(Stream<String> lines, Consumer<String> consumer,
                                         ProcessAiCancellation cancellation) {
        Accumulator result = new Accumulator(properties.getZhipuModel());
        List<String> nonSse = new ArrayList<>();
        try (lines; ProcessAiCancellation.Registration ignored = cancellation.onCancel(lines::close)) {
            lines.forEachOrdered(line -> consumeLine(line, nonSse, result, consumer, cancellation));
        }
        if (result.content.isEmpty() && !nonSse.isEmpty()) {
            consumeChunk(String.join("\n", nonSse), result, consumer);
        }
        if (result.content.isEmpty()) {
            throw failure("AI_PROVIDER_EMPTY_RESULT", true, "GLM provider returned no content");
        }
        return new ProcessAiModelResult(result.content.toString(), result.model,
                AiProvider.ZHIPU.name(), "FALLBACK", result.inputTokens, result.outputTokens);
    }

    private void consumeLine(String line, List<String> nonSse, Accumulator result,
                             Consumer<String> consumer, ProcessAiCancellation cancellation) {
        cancellation.throwIfCancelled();
        if (!line.startsWith("data:")) {
            if (!line.isBlank()) nonSse.add(line);
            return;
        }
        String data = line.substring(5).trim();
        if (!data.isBlank() && !"[DONE]".equals(data)) consumeChunk(data, result, consumer);
    }

    private void consumeChunk(String data, Accumulator result, Consumer<String> consumer) {
        try {
            JsonNode root = objectMapper.readTree(data);
            String model = root.path("model").asText(null);
            if (model != null) result.model = model;
            JsonNode usage = root.path("usage");
            if (usage.isObject()) {
                result.inputTokens = usage.path("prompt_tokens").asInt(0);
                result.outputTokens = usage.path("completion_tokens").asInt(0);
            }
            JsonNode choice = root.path("choices").path(0);
            JsonNode content = choice.path("delta").path("content");
            if (!content.isTextual()) content = choice.path("message").path("content");
            if (!content.isTextual() || content.asText().isEmpty()) return;
            result.content.append(content.asText());
            consumer.accept(content.asText());
        } catch (Exception exception) {
            throw failure("AI_PROVIDER_PROTOCOL_ERROR", true, "GLM response format is invalid");
        }
    }

    private String apiKey() {
        return credentialResolver.resolveApiKey(AiProvider.ZHIPU)
                .orElseThrow(() -> failure(
                        "AI_PROVIDER_NOT_CONFIGURED", false, "GLM API key is not configured"));
    }

    private void requireSuccess(int status) {
        if (status >= 200 && status < 300) return;
        if (status == 401 || status == 403) throw failure("AI_PROVIDER_AUTH_FAILED", false, "GLM authentication failed");
        if (status == 429) throw failure("AI_PROVIDER_RATE_LIMITED", true, "GLM rate limit reached");
        if (status >= 500) throw failure("AI_PROVIDER_UPSTREAM_ERROR", true, "GLM service failed");
        throw failure("AI_PROVIDER_REQUEST_REJECTED", false, "GLM rejected the request");
    }

    private ProcessAiProviderException cancelled() {
        return failure("AI_REQUEST_CANCELLED", false, "AI request was cancelled by the client");
    }

    private ProcessAiProviderException failure(String code, boolean retryable, String message) {
        return new ProcessAiProviderException(code, retryable, message);
    }

    private static final class Accumulator {
        private final StringBuilder content = new StringBuilder();
        private String model;
        private int inputTokens;
        private int outputTokens;

        private Accumulator(String model) {
            this.model = model;
        }
    }
}
