package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderPrintStageResolverTest {

    @ParameterizedTest
    @CsvSource({
            "0,DRAFT",
            "1,PENDING_ISSUE",
            "2,PENDING_MANUAL_CONFIRM",
            "3,WAITING_BACK_RECORD",
            "4,COMPLETED",
            "5,SETTLED",
            "6,VOIDED",
            "99,UNKNOWN"
    })
    void resolve_mapsOrderStatusToStablePrintStage(int status, ProcessOrderPrintStage expected) {
        ProcessOrder order = new ProcessOrder();
        order.setOrderStatus(status);

        ProcessOrderPrintStage actual = ProcessOrderPrintStageResolver.resolve(order);

        assertThat(actual).isEqualTo(expected);
    }
}
