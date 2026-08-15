package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemoryDocumentValidatorTest {

    @Test
    void rejectsTrailingJsonTokensInDatabaseContent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ProjectMemoryDocumentValidator validator = new ProjectMemoryDocumentValidator(
                objectMapper, new ProjectMemoryChecksum(objectMapper));
        String seed = Files.readString(Path.of("docs/ai/project-memory.seed.v1.json"));
        ProjectMemoryDocumentRow row = new ProjectMemoryDocumentRow(
                "memory-uuid", "1.0.0", "1.0", "sha256:" + "0".repeat(64),
                seed + "{}", "ACTIVE", null, "system", null);

        assertThatThrownBy(() -> validator.validateDatabaseRow(row))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid JSON");
    }
}
