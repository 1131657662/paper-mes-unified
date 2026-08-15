package com.paper.mes.ai.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectMemoryPatchOperation(
        @NotBlank @Pattern(regexp = "add|replace|remove") String op,
        @NotBlank @Size(max = 256) String path,
        JsonNode value) {
}
