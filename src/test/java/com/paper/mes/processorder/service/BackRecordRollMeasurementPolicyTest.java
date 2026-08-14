package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackRecordRollMeasurementPolicyTest {

    @Test
    void optionalMissingWeight_doesNotPromoteEstimatedWeightToMeasured() {
        OriginalRoll roll = roll("ESTIMATED", "1");

        boolean measured = BackRecordRollMeasurementPolicy.apply(roll, dto(null), false);

        assertThat(measured).isFalse();
        assertThat(roll.getWeightStatus()).isEqualTo("ESTIMATED");
        assertThat(BackRecordRollMeasurementPolicy.isMeasured(roll)).isFalse();
    }

    @Test
    void requiredMissingWeight_isRejected() {
        OriginalRoll roll = roll("UNKNOWN", null);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto(null), true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("复称实际重量");
    }

    @Test
    void positiveBackRecordWeight_isRecordedAsScaleMeasurement() {
        OriginalRoll roll = roll("ESTIMATED", "1");

        boolean measured = BackRecordRollMeasurementPolicy.apply(roll, dto("2000"), true);

        assertThat(measured).isTrue();
        assertThat(roll.getActualWeight()).isEqualByComparingTo("2000");
        assertThat(roll.getWeightStatus()).isEqualTo("MEASURED");
        assertThat(roll.getWeightSource()).isEqualTo("SCALE");
    }

    @Test
    void positiveLegacyWeightWithoutStatus_remainsCompatible() {
        assertThat(BackRecordRollMeasurementPolicy.isMeasured(roll(null, "1200"))).isTrue();
    }

    private OriginalRoll roll(String status, String actualWeight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setRollNo("R-001");
        roll.setWeightStatus(status);
        roll.setActualWeight(actualWeight == null ? null : new BigDecimal(actualWeight));
        return roll;
    }

    private BackRecordRollDTO dto(String actualWeight) {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid("roll-1");
        dto.setActualWeight(actualWeight == null ? null : new BigDecimal(actualWeight));
        return dto;
    }
}
