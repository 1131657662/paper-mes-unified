package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WeightStatus;

/** Applies the measurement semantics of one source roll during back-recording. */
public final class BackRecordRollMeasurementPolicy {

    private BackRecordRollMeasurementPolicy() {
    }

    public static boolean apply(OriginalRoll roll, BackRecordRollDTO dto, boolean weightRequired) {
        validate(dto, roll, weightRequired);
        roll.setActualGramWeight(dto.getActualGramWeight());
        roll.setActualWidth(dto.getActualWidth());
        if (!positive(dto.getActualWeight())) return false;
        roll.setActualWeight(dto.getActualWeight());
        roll.setWeightStatus(WeightStatus.MEASURED.name());
        roll.setWeightSource("SCALE");
        return true;
    }

    public static boolean isMeasured(OriginalRoll roll) {
        if (roll == null || !positive(roll.getActualWeight())) return false;
        String status = roll.getWeightStatus();
        return status == null || status.isBlank() || WeightStatus.MEASURED.name().equals(status);
    }

    private static void validate(BackRecordRollDTO dto, OriginalRoll roll, boolean weightRequired) {
        if (weightRequired && !positive(dto.getActualWeight())) {
            throw weightError(roll);
        }
        if (dto.getActualWeight() != null && !positive(dto.getActualWeight())) {
            throw weightError(roll);
        }
        if (dto.getActualGramWeight() != null && dto.getActualGramWeight() <= 0) {
            throw new BusinessException("原纸实际克重必须大于0：" + identity(roll));
        }
        if (dto.getActualWidth() != null && dto.getActualWidth() <= 0) {
            throw new BusinessException("原纸实际门幅必须大于0：" + identity(roll));
        }
    }

    private static boolean positive(java.math.BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BusinessException weightError(OriginalRoll roll) {
        return new BusinessException("原纸复称实际重量必须大于0：" + identity(roll));
    }

    private static String identity(OriginalRoll roll) {
        return roll.getRollNo() == null || roll.getRollNo().isBlank() ? roll.getUuid() : roll.getRollNo();
    }
}
