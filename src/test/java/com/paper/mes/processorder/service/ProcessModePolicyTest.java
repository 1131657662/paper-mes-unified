package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessModePolicyTest {

    @Test
    void serviceOnly_allowsMissingMainProcess() {
        assertDoesNotThrow(() -> ProcessModePolicy.requireValid(4, null));
        assertTrue(ProcessModePolicy.isServiceOnly(4));
    }

    @Test
    void standard_requiresSawOrRewind() {
        assertThrows(BusinessException.class, () -> ProcessModePolicy.requireValid(1, null));
        assertThrows(BusinessException.class, () -> ProcessModePolicy.requireValid(2, 3));
    }

    @Test
    void unknownMode_isRejected() {
        assertThrows(BusinessException.class, () -> ProcessModePolicy.requireValid(5, null));
    }

    @Test
    void serviceSteps_supportProcessingModesButExcludeDirectShip() {
        assertTrue(ProcessModePolicy.supportsServiceSteps(1));
        assertTrue(ProcessModePolicy.supportsServiceSteps(2));
        assertTrue(ProcessModePolicy.supportsServiceSteps(4));
        assertTrue(!ProcessModePolicy.supportsServiceSteps(3));
    }
}
