package com.paper.mes.ai.process.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.credential.AiProviderCredentialResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class DeepSeekProcessClient implements ProcessAiModelClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekProcessClient.class);
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final AiProviderCredentialResolver credentialResolver;
    private final HttpClient httpClient;

    @Autowired
    public DeepSeekProcessClient(AiProperties properties, ObjectMapper objectMapper,
                                 AiProviderCredentialResolver credentialResolver) {
        this(properties, objectMapper, credentialResolver, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getDeepseekConnectTimeoutMs()))
                .build());
    }

    DeepSeekProcessClient(AiProperties properties, ObjectMapper objectMapper,
                          AiProviderCredentialResolver credentialResolver,
                          HttpClient httpClient) {
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
        String apiKey = requireApiKey();
        Thread requestThread = Thread.currentThread();
        try (ProcessAiCancellation.Registration ignored = cancellation.onCancel(requestThread::interrupt)) {
            cancellation.throwIfCancelled();
            HttpResponse<Stream<String>> response = httpClient.send(request(prompt, apiKey),
                    HttpResponse.BodyHandlers.ofLines());
            cancellation.throwIfCancelled();
            requireSuccess(response.statusCode());
            return consume(response.body(), deltaConsumer, cancellation);
        } catch (ProcessAiProviderException ex) {
            throw ex;
        } catch (java.net.http.HttpTimeoutException ex) {
            throw failure("AI_PROVIDER_TIMEOUT", true, "AI供应商响应超时");
        } catch (InterruptedException ex) {
            if (cancellation.isCancelled()) {
                throw failure("AI_REQUEST_CANCELLED", false, "AI request was cancelled by the client");
            }
            Thread.currentThread().interrupt();
            throw failure("AI_PROVIDER_INTERRUPTED", true, "AI供应商调用已中断");
        } catch (Exception ex) {
            if (cancellation.isCancelled()) {
                throw failure("AI_REQUEST_CANCELLED", false, "AI request was cancelled by the client");
            }
            throw failure("AI_PROVIDER_UNAVAILABLE", true, "AI供应商暂不可用");
        }
    }

    private HttpRequest request(ProcessAiModelPrompt prompt, String apiKey) throws Exception {
        String baseUrl = properties.getDeepseekBaseUrl().replaceAll("/+$", "");
        return HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMillis(properties.getDeepseekReadTimeoutMs()))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body(prompt)))
                .build();
    }

    private String body(ProcessAiModelPrompt prompt) throws Exception {
        Map<String, Object> request = Map.of(
                "model", properties.getDeepseekModelPro(),
                "messages", List.of(
                        Map.of("role", "system", "content", prompt.systemInstruction()),
                        Map.of("role", "user", "content", prompt.userContext())),
                "temperature", 0,
                "max_tokens", properties.getDeepseekMaxOutputTokens(),
                "thinking", Map.of("type", "disabled"),
                "response_format", Map.of("type", "json_object"),
                "stream", true,
                "stream_options", Map.of("include_usage", true));
        return objectMapper.writeValueAsString(request);
    }

    private ProcessAiModelResult consume(Stream<String> lines, Consumer<String> deltaConsumer,
                                         ProcessAiCancellation cancellation) {
        DeepSeekStreamAccumulator result = new DeepSeekStreamAccumulator(
                properties.getDeepseekModelPro());
        List<String> nonSseLines = new java.util.ArrayList<>();
        try (lines; ProcessAiCancellation.Registration ignored = cancellation.onCancel(lines::close)) {
            lines.forEachOrdered(line -> {
                cancellation.throwIfCancelled();
                if (!line.startsWith("data:")) {
                    if (!line.isBlank()) nonSseLines.add(line);
                    return;
                }
                result.sawSseData = true;
                String data = line.substring(5).trim();
                if (!data.isBlank() && !"[DONE]".equals(data)) {
                    consumeChunk(data, result, deltaConsumer);
                }
            });
        }
        if (result.content.isEmpty() && !result.sawSseData && !nonSseLines.isEmpty()) {
            consumeChunk(String.join("\n", nonSseLines), result, deltaConsumer);
        }
        if (result.content.isEmpty()) {
            log.warn(
                    "DeepSeek response contained no usable content: sseData={}, chunks={}, reasoningChars={}, finishReason={}, model={}",
                    result.sawSseData,
                    result.chunkCount,
                    result.reasoningCharacters,
                    result.finishReason,
                    result.model);
        }
        if (result.content.isEmpty()) throw failure("AI_PROVIDER_EMPTY_RESULT", true, "AI供应商未返回内容");
        return new ProcessAiModelResult(result.content.toString(), result.model,
                AiProvider.DEEPSEEK.name(), "PRO", result.inputTokens, result.outputTokens);
    }

    private void consumeChunk(String data, DeepSeekStreamAccumulator result,
                              Consumer<String> deltaConsumer) {
        try {
            result.chunkCount++;
            JsonNode root = objectMapper.readTree(data);
            String modelValue = root.path("model").asText(null);
            if (modelValue != null) result.model = modelValue;
            JsonNode usage = root.path("usage");
            if (usage.isObject()) {
                result.inputTokens = usage.path("prompt_tokens").asInt(0);
                result.outputTokens = usage.path("completion_tokens").asInt(0);
            }
            JsonNode choice = root.path("choices").path(0);
            String finishReason = choice.path("finish_reason").asText(null);
            if (finishReason != null) result.finishReason = finishReason;
            JsonNode delta = choice.path("delta");
            JsonNode message = choice.path("message");
            appendContent(delta.path("content"), result, deltaConsumer);
            if (result.content.isEmpty()) appendContent(message.path("content"), result, deltaConsumer);
            JsonNode reasoning = delta.path("reasoning_content");
            if (!reasoning.isTextual()) reasoning = message.path("reasoning_content");
            if (reasoning.isTextual()) result.reasoningCharacters += reasoning.asText().length();
        } catch (ProcessAiProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failure("AI_PROVIDER_PROTOCOL_ERROR", true, "AI供应商流式响应格式无效");
        }
    }

    private String requireApiKey() {
        return credentialResolver.resolveApiKey(AiProvider.DEEPSEEK)
                .orElseThrow(() -> failure(
                        "AI_PROVIDER_NOT_CONFIGURED", false, "DeepSeek尚未配置"));
    }

    private void requireSuccess(int status) {
        if (status >= 200 && status < 300) return;
        if (status == 401 || status == 403) throw failure("AI_PROVIDER_AUTH_FAILED", false, "AI供应商认证失败");
        if (status == 402) throw failure("AI_PROVIDER_PAYMENT_REQUIRED", false, "AI供应商账户余额不足");
        if (status == 429) throw failure("AI_PROVIDER_RATE_LIMITED", true, "AI供应商限流");
        if (status >= 500) throw failure("AI_PROVIDER_UPSTREAM_ERROR", true, "AI供应商服务异常");
        throw failure("AI_PROVIDER_REQUEST_REJECTED", false, "AI供应商拒绝请求");
    }

    private ProcessAiProviderException failure(String code, boolean retryable, String message) {
        return new ProcessAiProviderException(code, retryable, message);
    }
    private void appendContent(JsonNode content, DeepSeekStreamAccumulator result,
                               Consumer<String> deltaConsumer) {
        if (!content.isTextual() || content.asText().isEmpty()) return;
        result.content.append(content.asText());
        deltaConsumer.accept(content.asText());
    }
}
