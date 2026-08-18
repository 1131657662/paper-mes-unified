package com.paper.mes.ai.process.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.service.ProcessPlanMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessAiDraftBaselineReaderTest {

    @Test
    void readFreezesTheSavedSinglePlanForItsStableRollReference() throws Exception {
        ProcessConfigDraftMapper mapper = mock(ProcessConfigDraftMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessAiDraftBaselineReader reader = new ProcessAiDraftBaselineReader(
                mapper, objectMapper, new ProcessPlanMapper());
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(2);
        plan.setMainStepType(2);
        plan.setRewindMode(6);
        ProcessConfigDraft draft = new ProcessConfigDraft();
        draft.setOriginalUuid("roll-1");
        draft.setConfigJson(objectMapper.writeValueAsString(plan));
        when(mapper.selectList(any())).thenReturn(List.of(draft));

        List<ProcessAiBaselinePlan> result = reader.read(
                "order-1", List.of(roll("R1", "roll-1")));

        assertThat(result).singleElement().satisfies(baseline -> {
            assertThat(baseline.ownerRollRef()).isEqualTo("R1");
            assertThat(baseline.route()).isFalse();
            assertThat(baseline.plan().getRewindMode()).isEqualTo(6);
        });
    }

    private ProcessAiRollContext roll(String ref, String uuid) {
        return new ProcessAiRollContext(
                ref, uuid, 1, "paper", 250, 2_000,
                1_500, 3, null, 1, 2, 2);
    }
}
