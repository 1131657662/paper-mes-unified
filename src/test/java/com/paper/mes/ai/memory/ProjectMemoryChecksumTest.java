package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemoryChecksumTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectMemoryChecksum checksum = new ProjectMemoryChecksum(objectMapper);

    @Test
    void repositorySeedMatchesItsDeclaredChecksum() throws Exception {
        JsonNode document = objectMapper.readTree(
                Path.of("docs/ai/project-memory.seed.v1.json").toFile());

        assertThat(checksum.calculate(document))
                .as("actual checksum")
                .isEqualTo(document.path("checksum").asText());
        checksum.requireValid(document);
    }

    @Test
    void packagedRuntimeSeedCannotDriftFromTheReviewedSeed() throws Exception {
        JsonNode repositorySeed = objectMapper.readTree(
                Path.of("docs/ai/project-memory.seed.v1.json").toFile());
        JsonNode runtimeSeed = objectMapper.readTree(
                Path.of("src/main/resources/ai/project-memory.seed.v1.json").toFile());

        assertThat(runtimeSeed).isEqualTo(repositorySeed);
        checksum.requireValid(runtimeSeed);
    }

    @Test
    void changedContentIsRejectedAgainstTheOriginalChecksum() throws Exception {
        ObjectNode document = (ObjectNode) objectMapper.readTree(
                Path.of("docs/ai/project-memory.seed.v1.json").toFile());
        document.put("project", "tampered-project");

        assertThatThrownBy(() -> checksum.requireValid(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checksum");
    }
}
