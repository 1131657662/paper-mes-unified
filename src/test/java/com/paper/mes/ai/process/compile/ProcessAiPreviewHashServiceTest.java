package com.paper.mes.ai.process.compile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiPreviewHashServiceTest {

    @Test
    void hashIsStableForEquivalentObjectFieldOrder() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessAiPreviewHashService service = new ProcessAiPreviewHashService(mapper);
        ProcessAiPreviewHashInput first = input(mapper.readTree("{\"b\":2,\"a\":1}"));
        ProcessAiPreviewHashInput second = input(mapper.readTree("{\"a\":1,\"b\":2}"));

        assertThat(service.hash(first)).isEqualTo(service.hash(second));
    }

    private ProcessAiPreviewHashInput input(com.fasterxml.jackson.databind.JsonNode plans) {
        return new ProcessAiPreviewHashInput("order", 3, "conversation", 2,
                "1.0", "sha256:" + "a".repeat(64), "r", "e", "c",
                List.of("REWIND_FINISH_CORE_3_INCH"), plans,
                new ObjectMapper().createObjectNode());
    }
}
