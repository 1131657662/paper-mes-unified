package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettleOriginalWeightStatusTest {

    @Test
    void resolve_actualWeightAlwaysMeansMeasured() {
        OriginalRoll roll = roll("ESTIMATED", "1");
        roll.setActualWeight(new BigDecimal("2000"));

        assertThat(SettleOriginalWeightStatus.resolve(roll)).isEqualTo("MEASURED");
    }

    @Test
    void resolve_preservesExplicitUnknownAndEstimatedEntryMeaning() {
        assertThat(SettleOriginalWeightStatus.resolve(roll("UNKNOWN", null))).isEqualTo("UNKNOWN");
        assertThat(SettleOriginalWeightStatus.resolve(roll("ESTIMATED", "1"))).isEqualTo("ESTIMATED");
    }

    @Test
    void resolve_legacyWeightInfersReferenceWithoutClaimingMeasurement() {
        assertThat(SettleOriginalWeightStatus.resolve(roll(null, "3"))).isEqualTo("ESTIMATED");
        assertThat(SettleOriginalWeightStatus.resolve(roll(null, null))).isEqualTo("UNKNOWN");
    }

    private OriginalRoll roll(String status, String weight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setWeightStatus(status);
        roll.setRollWeight(weight == null ? null : new BigDecimal(weight));
        return roll;
    }
}
