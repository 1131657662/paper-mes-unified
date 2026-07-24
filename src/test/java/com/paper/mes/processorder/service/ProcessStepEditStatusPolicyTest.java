package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessStepEditStatusPolicyTest {

    @Test
    void allowsExtraServiceStepsWhileDrafting() {
        assertDoesNotThrow(() -> ProcessStepEditStatusPolicy.requireAddAllowed(0, true));
        assertDoesNotThrow(() -> ProcessStepEditStatusPolicy.requireChangeAllowed(0));
    }

    @Test
    void keepsMainProcessInsideTheDraftPlan() {
        assertThrows(BusinessException.class,
                () -> ProcessStepEditStatusPolicy.requireAddAllowed(0, false));
    }

    @Test
    void preservesPendingAndToRecordRules() {
        assertDoesNotThrow(() -> ProcessStepEditStatusPolicy.requireAddAllowed(1, false));
        assertDoesNotThrow(() -> ProcessStepEditStatusPolicy.requireAddAllowed(3, true));
        assertThrows(BusinessException.class,
                () -> ProcessStepEditStatusPolicy.requireChangeAllowed(3));
    }
}
