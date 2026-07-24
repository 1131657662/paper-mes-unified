package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceOnlyProcessPolicyTest {

    private final ProcessStepMapper stepMapper = mock(ProcessStepMapper.class);
    private final ServiceOnlyProcessPolicy policy = new ServiceOnlyProcessPolicy(stepMapper);

    @Test
    void configuredServiceStep_isAccepted() {
        when(stepMapper.selectCount(any())).thenReturn(1L);

        assertTrue(policy.hasConfiguredStep("roll-1"));
        assertDoesNotThrow(() -> policy.requireConfiguredStep("roll-1"));
    }

    @Test
    void missingServiceStep_isRejected() {
        when(stepMapper.selectCount(any())).thenReturn(0L);

        assertFalse(policy.hasConfiguredStep("roll-1"));
        assertThrows(BusinessException.class, () -> policy.requireConfiguredStep("roll-1"));
    }
}
