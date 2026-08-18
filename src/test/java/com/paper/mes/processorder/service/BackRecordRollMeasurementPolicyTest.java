package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WeightEntryMode;
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
    void missingOptionalActualSpecs_preservePreviouslyRecordedValues() {
        OriginalRoll roll = roll("ESTIMATED", "1");
        roll.setActualGramWeight(180);
        roll.setActualWidth(1250);

        BackRecordRollMeasurementPolicy.apply(roll, dto("2000"), true);

        assertThat(roll.getActualGramWeight()).isEqualTo(180);
        assertThat(roll.getActualWidth()).isEqualTo(1250);
    }

    @Test
    void positiveLegacyWeightWithoutStatus_remainsCompatible() {
        assertThat(BackRecordRollMeasurementPolicy.isMeasured(roll(null, "1200"))).isTrue();
    }

    @Test
    void carryNominalWeight_staysEstimatedAndUsesSourceTotal() {
        OriginalRoll roll = roll("ESTIMATED", null);
        roll.setRollWeight(new BigDecimal("2000"));
        roll.setPieceNum(2);
        roll.setWeightRecordedAt(java.time.LocalDateTime.now());
        roll.setWeightRecordedBy("old-user");
        BackRecordRollDTO dto = dto(null);
        dto.setWeightEntryMode(WeightEntryMode.CARRY_NOMINAL);

        boolean measured = BackRecordRollMeasurementPolicy.apply(roll, dto, true);

        assertThat(measured).isFalse();
        assertThat(roll.getActualWeight()).isEqualByComparingTo("4000");
        assertThat(roll.getWeightStatus()).isEqualTo("ESTIMATED");
        assertThat(roll.getWeightSource()).isEqualTo("CARRIED_NOMINAL");
        assertThat(roll.getWeightRecordedAt()).isNull();
        assertThat(roll.getWeightRecordedBy()).isNull();
        assertThat(BackRecordRollMeasurementPolicy.isMeasured(roll)).isFalse();
    }

    @Test
    void carryNominalWeight_withoutReference_isRejected() {
        OriginalRoll roll = roll("UNKNOWN", null);
        BackRecordRollDTO dto = dto(null);
        dto.setWeightEntryMode(WeightEntryMode.CARRY_NOMINAL);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实际重量");
    }

    @Test
    void carryNominalWeight_doesNotUseLegacyPlaceholderForUnknownRoll() {
        OriginalRoll roll = roll("UNKNOWN", null);
        roll.setRollWeight(new BigDecimal("1"));
        BackRecordRollDTO dto = dto(null);
        dto.setWeightEntryMode(WeightEntryMode.CARRY_NOMINAL);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实际重量");
    }

    @Test
    void confirmReferenceWeight_promotesCurrentReferenceToMeasured() {
        OriginalRoll roll = roll("ESTIMATED", "1");
        BackRecordRollDTO dto = dto("1");
        dto.setWeightEntryMode(WeightEntryMode.CONFIRM_REFERENCE);

        boolean measured = BackRecordRollMeasurementPolicy.apply(roll, dto, true);

        assertThat(measured).isTrue();
        assertThat(roll.getActualWeight()).isEqualByComparingTo("1");
        assertThat(roll.getWeightStatus()).isEqualTo("MEASURED");
        assertThat(roll.getWeightSource()).isEqualTo("MANUAL_CONFIRM");
        assertThat(BackRecordRollMeasurementPolicy.isMeasured(roll)).isTrue();
    }

    @Test
    void confirmReferenceWeight_usesServerNominalValue() {
        OriginalRoll roll = roll("ESTIMATED", null);
        roll.setRollWeight(new BigDecimal("2000"));
        roll.setPieceNum(2);
        BackRecordRollDTO dto = dto("4000");
        dto.setWeightEntryMode(WeightEntryMode.CONFIRM_REFERENCE);

        boolean measured = BackRecordRollMeasurementPolicy.apply(roll, dto, true);

        assertThat(measured).isTrue();
        assertThat(roll.getActualWeight()).isEqualByComparingTo("4000");
        assertThat(roll.getWeightStatus()).isEqualTo("MEASURED");
        assertThat(roll.getWeightSource()).isEqualTo("MANUAL_CONFIRM");
    }

    @Test
    void confirmReferenceWeight_rejectsClientValueDifferentFromServerReference() {
        OriginalRoll roll = roll("ESTIMATED", "1");
        BackRecordRollDTO dto = dto("2");
        dto.setWeightEntryMode(WeightEntryMode.CONFIRM_REFERENCE);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参考重量");
        assertThat(roll.getWeightStatus()).isEqualTo("ESTIMATED");
        assertThat(roll.getWeightSource()).isNull();
    }

    @Test
    void confirmReferenceWeight_withoutReferenceIsRejected() {
        OriginalRoll roll = roll("UNKNOWN", null);
        BackRecordRollDTO dto = dto("1");
        dto.setWeightEntryMode(WeightEntryMode.CONFIRM_REFERENCE);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参考重量");
    }

    @Test
    void userEstimate_withoutPositiveWeight_isRejected() {
        OriginalRoll roll = roll("UNKNOWN", null);
        BackRecordRollDTO dto = dto(null);
        dto.setWeightEntryMode(WeightEntryMode.USER_ESTIMATE);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实际重量");
    }

    @Test
    void userEstimate_clearsPreviousMeasurementAudit() {
        OriginalRoll roll = roll("MEASURED", "1200");
        roll.setWeightRecordedAt(java.time.LocalDateTime.now());
        roll.setWeightRecordedBy("scale-user");
        BackRecordRollDTO dto = dto("1300");
        dto.setWeightEntryMode(WeightEntryMode.USER_ESTIMATE);

        boolean measured = BackRecordRollMeasurementPolicy.apply(roll, dto, true);

        assertThat(measured).isFalse();
        assertThat(roll.getWeightStatus()).isEqualTo("ESTIMATED");
        assertThat(roll.getWeightRecordedAt()).isNull();
        assertThat(roll.getWeightRecordedBy()).isNull();
    }

    @Test
    void measuredMode_withoutPositiveWeight_isRejected() {
        OriginalRoll roll = roll("UNKNOWN", null);
        BackRecordRollDTO dto = dto(null);
        dto.setWeightEntryMode(WeightEntryMode.MEASURED);

        assertThatThrownBy(() -> BackRecordRollMeasurementPolicy.apply(roll, dto, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实际重量");
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
