package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateEvidenceResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemoryCandidateEvidenceResponseTest {

    @Test
    void evidenceResponseDoesNotExposeInternalOperatorIdentity() throws Exception {
        ProjectMemoryCandidateEvidenceResponse response =
                new ProjectMemoryCandidateEvidenceResponse(
                        "evidence-1", "core=3inch", "AI_CONFIRMED", null,
                        null, null, true, LocalDateTime.now());

        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(response);

        assertThat(json).doesNotContain("createdBy", "created_by");
    }
}
