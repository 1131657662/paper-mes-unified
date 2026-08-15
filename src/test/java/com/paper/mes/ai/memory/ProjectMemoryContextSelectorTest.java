package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemoryContextSelectorTest {

    @Test
    void selectedContextRedactsAmountsAndSensitiveFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var document = mapper.readTree("""
                {"rules":{"film":{"status":"ACTIVE","scope":"PACKAGING","keywords":["包膜"],
                "input":"包膜加20元每件","unitPrice":20,"answer":"repack"}},
                "terms":{},"examples":{}}
                """);
        ProjectMemorySnapshot snapshot = new ProjectMemorySnapshot("1.0.0", "1.0", "sha256:test", document,
                Instant.now());

        String context = new ProjectMemoryContextSelector().select(snapshot, "请处理包膜", "process-orders", 4_000);

        assertThat(context).contains("film", "[金额]").doesNotContain("20元", "unitPrice");
    }
}
