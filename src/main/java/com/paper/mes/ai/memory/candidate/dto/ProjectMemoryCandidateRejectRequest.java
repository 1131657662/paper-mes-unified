package com.paper.mes.ai.memory.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectMemoryCandidateRejectRequest(
        @NotBlank @Size(max = 500) String reason) {
}
