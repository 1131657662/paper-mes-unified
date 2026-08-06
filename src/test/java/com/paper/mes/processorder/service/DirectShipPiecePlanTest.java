package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirectShipPiecePlanTest {

    @Test
    void from_whenWeightCanBeSplit_closesRoundingOnLastPiece() {
        OriginalRoll source = source(3, "1.000");

        DirectShipPiecePlan plan = DirectShipPiecePlan.from(source);

        assertEquals(List.of(
                new BigDecimal("0.333"),
                new BigDecimal("0.333"),
                new BigDecimal("0.334")), plan.weights());
    }

    @Test
    void from_whenWeightCannotGiveEveryPiecePositiveWeight_rejectsPlan() {
        OriginalRoll source = source(500, "0.001");

        BusinessException exception = assertThrows(
                BusinessException.class, () -> DirectShipPiecePlan.from(source));

        assertEquals("直发复称总重量不足以按件分配，每件重量必须大于0", exception.getMessage());
    }

    private OriginalRoll source(int pieceNum, String actualWeight) {
        OriginalRoll source = new OriginalRoll();
        source.setPieceNum(pieceNum);
        source.setActualWeight(new BigDecimal(actualWeight));
        return source;
    }
}
