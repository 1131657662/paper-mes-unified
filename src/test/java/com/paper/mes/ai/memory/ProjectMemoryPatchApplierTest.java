package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchOperation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemoryPatchApplierTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectMemoryPatchApplier applier = new ProjectMemoryPatchApplier();

    @Test
    void appliesAllowlistedNestedReplaceAndNewEntry() throws Exception {
        ObjectNode source = (ObjectNode) mapper.readTree(
                "{\"rules\":{\"r1\":{\"status\":\"ACTIVE\"}},\"terms\":{}}");

        ObjectNode changed = applier.apply(source, List.of(
                new ProjectMemoryPatchOperation("replace", "/rules/r1/status", mapper.readTree("\"INACTIVE\"")),
                new ProjectMemoryPatchOperation("add", "/terms/t-new", mapper.readTree(
                        "{\"type\":\"TERM\",\"status\":\"ACTIVE\",\"phrase\":\"新术语\"}"))));

        assertThat(changed.at("/rules/r1/status").asText()).isEqualTo("INACTIVE");
        assertThat(changed.at("/terms/t-new/phrase").asText()).isEqualTo("新术语");
        assertThat(source.at("/rules/r1/status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsMetadataAndUnsupportedOperationPaths() throws Exception {
        ObjectNode source = (ObjectNode) mapper.readTree("{\"rules\":{\"r1\":{}}}");

        assertThatThrownBy(() -> applier.apply(source, List.of(
                new ProjectMemoryPatchOperation("replace", "/memoryVersion", mapper.readTree("\"2.0.0\"")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MEMORY_PATCH_INVALID_PATH");
    }

    @Test
    void rejectsUnknownOperationBeforeMutation() throws Exception {
        ObjectNode source = (ObjectNode) mapper.readTree("{\"rules\":{\"r1\":{}}}");

        assertThatThrownBy(() -> applier.apply(source, List.of(
                new ProjectMemoryPatchOperation("copy", "/rules/r1/status", mapper.readTree("\"ACTIVE\"")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MEMORY_PATCH_INVALID");
    }
}
