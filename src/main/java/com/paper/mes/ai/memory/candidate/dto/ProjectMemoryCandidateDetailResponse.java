package com.paper.mes.ai.memory.candidate.dto;

import java.util.List;

public record ProjectMemoryCandidateDetailResponse(
        ProjectMemoryCandidateResponse candidate,
        List<ProjectMemoryCandidateEvidenceResponse> evidence) {

    public ProjectMemoryCandidateDetailResponse {
        evidence = List.copyOf(evidence);
    }
}
