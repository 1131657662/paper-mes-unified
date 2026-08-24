package com.paper.mes.ai.process.compile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ProcessAiPreviewHashService {

    private final ObjectMapper objectMapper;

    public ProcessAiPreviewHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(ProcessAiPreviewHashInput input) {
        try {
            JsonNode normalized = canonical(objectMapper.valueToTree(input));
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(normalized));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("AI preview hash could not be calculated", ex);
        }
    }

    private JsonNode canonical(JsonNode value) {
        if (value == null || value.isNull() || value.isValueNode()) return value;
        if (value.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            value.forEach(item -> result.add(canonical(item)));
            return result;
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        Map<String, JsonNode> sorted = new TreeMap<>();
        value.fields().forEachRemaining(entry -> sorted.put(entry.getKey(), canonical(entry.getValue())));
        sorted.forEach(result::set);
        return result;
    }
}
