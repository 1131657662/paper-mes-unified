package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessOrderFeeCalculationStatusTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void feeCalculation_allowsEditableAndExecutableStatuses(int status) {
        assertDoesNotThrow(() -> ProcessOrderServiceImpl.requireFeeCalculationStatus(status));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    void feeCalculation_rejectsLockedStatuses(int status) {
        assertThrows(BusinessException.class,
                () -> ProcessOrderServiceImpl.requireFeeCalculationStatus(status));
    }
}
