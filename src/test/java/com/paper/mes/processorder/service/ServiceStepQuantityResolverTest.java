package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceStepQuantityResolverTest {

    @Test
    void pieceBasis_usesSourcePieceCount() {
        OriginalRoll roll = roll();
        roll.setPieceNum(3);

        BigDecimal quantity = ServiceStepQuantityResolver.resolve("PIECE", roll);

        assertEquals(new BigDecimal("3"), quantity);
    }

    @Test
    void tonBasis_prefersBackRecordedActualWeight() {
        OriginalRoll roll = roll();
        roll.setActualWeight(new BigDecimal("2213"));

        BigDecimal quantity = ServiceStepQuantityResolver.resolve("TON", roll);

        assertEquals(new BigDecimal("2.213"), quantity);
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setPieceNum(2);
        roll.setRollWeight(new BigDecimal("1000"));
        roll.setTotalWeight(new BigDecimal("2000"));
        return roll;
    }
}
