package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessStepRouteMutationGuardTest {

    private ProcessStepMapper mapper;
    private ProcessStepRouteMutationGuard guard;

    @BeforeEach
    void setUp() {
        mapper = mock(ProcessStepMapper.class);
        guard = new ProcessStepRouteMutationGuard(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inputType", "inputOutputUuid", "parentStepUuid", "stageLevel"})
    void ordinaryMutation_withChainTopology_isRejectedWithoutQuery(String topologyField) {
        ProcessStep step = step();
        applyChainTopology(step, topologyField);

        assertThrows(BusinessException.class, () -> guard.requireOrdinaryMutationAllowed(step));

        verifyNoInteractions(mapper);
    }

    @Test
    void ordinaryMutation_withActiveRouteReference_isRejected() {
        ProcessStep step = step();
        when(mapper.hasActiveRouteReferences("step-1")).thenReturn(true);

        assertThrows(BusinessException.class, () -> guard.requireOrdinaryMutationAllowed(step));
    }

    @Test
    void ordinaryMutation_withoutRouteOwnership_isAllowed() {
        ProcessStep step = step();
        when(mapper.hasActiveRouteReferences("step-1")).thenReturn(false);

        assertDoesNotThrow(() -> guard.requireOrdinaryMutationAllowed(step));
    }

    private ProcessStep step() {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setInputType(1);
        step.setStageLevel(1);
        return step;
    }

    private void applyChainTopology(ProcessStep step, String topologyField) {
        switch (topologyField) {
            case "inputType" -> step.setInputType(2);
            case "inputOutputUuid" -> step.setInputOutputUuid("output-1");
            case "parentStepUuid" -> step.setParentStepUuid("parent-1");
            case "stageLevel" -> step.setStageLevel(2);
            default -> throw new IllegalArgumentException("Unknown topology field: " + topologyField);
        }
    }
}
