package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderPrintConfirmationPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "1,1,true",
            "1,0,false",
            "0,1,false",
            "0,0,false"
    })
    void isConfirmed_requiresConfirmedStatusAndPositivePrintCount(
            int printStatus, int printCount, boolean expected) {
        ProcessOrder order = new ProcessOrder();
        order.setPrintStatus(printStatus);
        order.setPrintCount(printCount);

        boolean actual = ProcessOrderPrintConfirmationPolicy.isConfirmed(order);

        assertThat(actual).isEqualTo(expected);
    }
}
