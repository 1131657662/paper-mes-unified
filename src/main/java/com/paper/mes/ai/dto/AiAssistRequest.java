package com.paper.mes.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiAssistRequest(
        @NotBlank @Size(max = 2_000) String question,
        @NotBlank @Pattern(regexp = "[a-z0-9-]{1,64}") String pageTemplate,
        @NotBlank @Pattern(regexp = "[0-9A-Za-z-]{1,64}") String contextEpoch) {
}
