package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.dto.OrderSettlementMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessRollSplitFactoryTest {

    @Test
    void orderRequest_preservesLegacyNominalWeightAsEstimated() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-001");
        OriginalRoll roll = roll();
        roll.setRollWeight(new BigDecimal("800"));
        roll.setWeightStatus(null);

        var request = ProcessRollSplitFactory.orderRequest(order, roll);

        assertThat(request.getOriginalRolls()).hasSize(1);
        assertThat(request.getOriginalRolls().getFirst().getRollWeight())
                .isEqualByComparingTo("800");
        assertThat(request.getOriginalRolls().getFirst().getWeightStatus()).hasToString("ESTIMATED");
    }

    @Test
    void orderRequest_keepsUnknownWeightUnknown() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-001");
        OriginalRoll roll = roll();
        roll.setRollWeight(null);
        roll.setWeightStatus("UNKNOWN");

        var request = ProcessRollSplitFactory.orderRequest(order, roll);

        assertThat(request.getOriginalRolls().getFirst().getRollWeight()).isNull();
        assertThat(request.getOriginalRolls().getFirst().getWeightStatus()).hasToString("UNKNOWN");
    }

    @Test
    void orderRequest_resetsMeasuredWeightForTargetOrderReweighing() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-001");
        OriginalRoll roll = roll();
        roll.setRollWeight(new BigDecimal("1"));
        roll.setActualWeight(new BigDecimal("2000"));
        roll.setWeightStatus("MEASURED");

        var request = ProcessRollSplitFactory.orderRequest(order, roll);

        assertThat(request.getOriginalRolls().getFirst().getRollWeight()).isNull();
        assertThat(request.getOriginalRolls().getFirst().getWeightStatus()).hasToString("UNKNOWN");
    }

    @Test
    void orderRequest_preservesOverrideSettlementIntentForTargetOrder() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-001");
        order.setSettleType(1);
        order.setSettleSource(OrderSettlementMode.OVERRIDE.name());
        order.setSettleCustomerVersion(12);
        order.setSettleOverrideReason("客户临时改为现结");

        var request = ProcessRollSplitFactory.orderRequest(order, roll());

        assertThat(request.getSettleMode()).isEqualTo(OrderSettlementMode.OVERRIDE);
        assertThat(request.getCustomerVersion()).isEqualTo(12);
        assertThat(request.getSettleType()).isEqualTo(1);
        assertThat(request.getSettleOverrideReason()).isEqualTo("客户临时改为现结");
    }

    @Test
    void orderRequest_preservesInheritedSettlementIntentForTargetOrder() {
        ProcessOrder order = new ProcessOrder();
        order.setOrderNo("JG-001");
        order.setSettleType(2);
        order.setSettleDay(15);
        order.setSettleSource(OrderSettlementMode.INHERIT.name());
        order.setSettleCustomerVersion(9);

        var request = ProcessRollSplitFactory.orderRequest(order, roll());

        assertThat(request.getSettleMode()).isEqualTo(OrderSettlementMode.INHERIT);
        assertThat(request.getCustomerVersion()).isEqualTo(9);
        assertThat(request.getSettleDay()).isEqualTo(15);
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setPaperName("牛卡纸");
        roll.setGramWeight(265);
        roll.setOriginalWidth(1200);
        roll.setPieceNum(1);
        return roll;
    }
}
