package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemoryManualCandidateFactoryTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectMemoryManualCandidateFactory factory =
            new ProjectMemoryManualCandidateFactory(mapper);

    @Test
    void createsReviewableExampleFromSubmittedManualConfiguration() {
        var finalConfiguration = mapper.createObjectNode();
        finalConfiguration.putArray("processPlans").addObject().put("mainStepType", 1);
        var snapshot = new ProjectMemorySubmissionLearningSnapshot(
                "order-1", "1.0.0", "1000的9件切900，3件切850",
                mapper.createObjectNode(), finalConfiguration, "admin");

        var proposal = factory.create(snapshot, memory()).orElseThrow();

        assertThat(proposal.candidateType()).isEqualTo("EXAMPLE");
        assertThat(proposal.document().path("input").asText())
                .isEqualTo("已确认工艺配置:/1");
        assertThat(proposal.document().path("input").asText())
                .doesNotContain("1000", "900", "客户");
        assertThat(proposal.document().path("expected").path("processType").asText())
                .isEqualTo("PROCESS_ORDER");
        assertThat(proposal.document().path("expected").path("field").asText())
                .isEqualTo("processPlans");
    }

    private ProjectMemorySnapshot memory() {
        var document = mapper.createObjectNode();
        document.set("examples", mapper.createObjectNode());
        return new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:" + "a".repeat(64), document, Instant.now());
    }
}
