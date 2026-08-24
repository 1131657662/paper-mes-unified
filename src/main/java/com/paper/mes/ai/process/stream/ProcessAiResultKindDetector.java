package com.paper.mes.ai.process.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessAiResultKindDetector {

    private final ObjectMapper objectMapper;

    ProcessAiResultKind detect(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            return "2.0".equals(root.path("schemaVersion").asText())
                    ? ProcessAiResultKind.UNDERSTANDING : ProcessAiResultKind.EXTRACTION;
        } catch (Exception ignored) {
            return ProcessAiResultKind.EXTRACTION;
        }
    }

    enum ProcessAiResultKind { EXTRACTION, UNDERSTANDING }
}
