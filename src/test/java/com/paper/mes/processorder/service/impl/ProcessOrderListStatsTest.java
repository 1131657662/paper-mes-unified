package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessOrderListStatsTest {

    @Test
    void apply_excludesSpareTrimAndVoidedFinishesFromFormalTotals() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll first = original(2, "100", null);
        OriginalRoll second = original(null, "90", "80");
        FinishRoll actual = finish(0, 0, 1, "50", "45");
        FinishRoll estimated = finish(null, null, null, "55", null);
        FinishRoll spare = finish(1, 0, 1, "10", null);
        FinishRoll trim = finish(0, 1, 1, "5", null);
        FinishRoll voided = finish(0, 0, 3, "20", "20");

        ProcessOrderListStats.apply(
                order,
                List.of(first, second),
                List.of(actual, estimated, spare, trim, voided));

        assertEquals(2, order.getOriginalRollCount());
        assertEquals(3, order.getOriginalPieceCount());
        assertEquals(new BigDecimal("180"), order.getOriginalRollWeight());
        assertEquals(2, order.getFinishRollCount());
        assertEquals(new BigDecimal("100"), order.getFinishRollWeight());
        assertEquals(new BigDecimal("105"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("45"), order.getActualFinishWeight());
        assertEquals(1, order.getSpareRollCount());
    }

    @Test
    void apply_excludesScrappedFinishesFromFormalAndSpareTotals() {
        ProcessOrder order = new ProcessOrder();
        FinishRoll formal = finish(0, 0, 1, "20", "15");
        formal.setFinishStatus(4);
        FinishRoll spare = finish(1, 0, 1, "10", null);
        spare.setFinishStatus(4);

        ProcessOrderListStats.apply(order, List.of(), List.of(formal, spare));

        assertEquals(0, order.getFinishRollCount());
        assertEquals(BigDecimal.ZERO, order.getFinishRollWeight());
        assertEquals(BigDecimal.ZERO, order.getEstimateFinishWeight());
        assertEquals(BigDecimal.ZERO, order.getActualFinishWeight());
        assertEquals(0, order.getSpareRollCount());
    }

    private OriginalRoll original(Integer pieces, String totalWeight, String actualWeight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setPieceNum(pieces);
        roll.setTotalWeight(decimal(totalWeight));
        roll.setActualWeight(decimal(actualWeight));
        return roll;
    }

    private FinishRoll finish(
            Integer spare,
            Integer remain,
            Integer rollNoStatus,
            String estimateWeight,
            String actualWeight) {
        FinishRoll roll = new FinishRoll();
        roll.setIsSpare(spare);
        roll.setIsRemain(remain);
        roll.setRollNoStatus(rollNoStatus);
        roll.setEstimateWeight(decimal(estimateWeight));
        roll.setActualWeight(decimal(actualWeight));
        return roll;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
