package com.paper.mes.settle.service.impl;

import com.paper.mes.settle.entity.SettleOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettleListSummaryAccumulatorTest {

    @Test
    void usesNormalizedAmountsAndExcludesVoidedDocumentsFromMoneyTotals() {
        SettleOrder active = settlement(2, "500", "225", "275", "25");
        SettleOrder voided = settlement(4, "900", "0", "900", "0");

        var result = SettleListSummaryAccumulator.summarize(List.of(active, voided));

        assertThat(result.totalDocumentCount()).isEqualTo(2);
        assertThat(result.partialDocumentCount()).isEqualTo(1);
        assertThat(result.voidDocumentCount()).isEqualTo(1);
        assertThat(result.activeTotalAmount()).isEqualByComparingTo("500");
        assertThat(result.activeReceivedAmount()).isEqualByComparingTo("225");
        assertThat(result.activeDiscountAmount()).isEqualByComparingTo("25");
    }

    private SettleOrder settlement(int status, String total, String received,
                                   String unreceived, String discount) {
        SettleOrder order = new SettleOrder();
        order.setSettleStatus(status);
        order.setTotalAmount(new BigDecimal(total));
        order.setReceivedAmount(new BigDecimal(received));
        order.setUnreceivedAmount(new BigDecimal(unreceived));
        order.setDiscountAmount(new BigDecimal(discount));
        return order;
    }
}
