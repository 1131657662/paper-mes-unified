package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemoryLearningHashStabilityTest {

    @Test
    void learningSnapshotsKeepTheSameHashWhenOutboxPayloadIsReadBack() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemorySubmissionLearningSnapshot original =
                new ProjectMemorySubmissionLearningSnapshot("order-1", "1.0.0", "客户要求",
                        mapper.createObjectNode(), mapper.createObjectNode(), "admin");

        ProjectMemorySubmissionLearningSnapshot restored = mapper.readValue(
                mapper.writeValueAsString(original), ProjectMemorySubmissionLearningSnapshot.class);

        assertThat(restored.customerRequirementHash()).isEqualTo(original.customerRequirementHash());
    }
}
