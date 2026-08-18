package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WeightEntryMode;
import com.paper.mes.processorder.model.WeightStatus;

import java.math.BigDecimal;

/** Applies the measurement semantics of one source roll during back-recording. */
public final class BackRecordRollMeasurementPolicy {

    private BackRecordRollMeasurementPolicy() {
    }

    public static boolean apply(OriginalRoll roll, BackRecordRollDTO dto, boolean weightRequired) {
        validate(dto, roll, weightRequired);
        if (dto.getActualGramWeight() != null) {
            roll.setActualGramWeight(dto.getActualGramWeight());
        }
        if (dto.getActualWidth() != null) {
            roll.setActualWidth(dto.getActualWidth());
        }
        if (dto.getWeightEntryMode() == WeightEntryMode.CARRY_NOMINAL) {
            BigDecimal nominal = nominalWeight(roll);
            if (!positive(nominal)) {
                throw weightError(roll);
            }
            roll.setActualWeight(nominal);
            roll.setWeightStatus(WeightStatus.ESTIMATED.name());
            roll.setWeightSource("CARRIED_NOMINAL");
            clearMeasurementAudit(roll);
            return false;
        }
        if (dto.getWeightEntryMode() == WeightEntryMode.CONFIRM_REFERENCE) {
            BigDecimal reference = referenceWeight(roll);
            if (!positive(reference) || !positive(dto.getActualWeight())
                    || dto.getActualWeight().compareTo(reference) != 0) {
                throw new BusinessException("确认重量与当前母卷参考重量不一致，请刷新后重试：" + identity(roll));
            }
            roll.setActualWeight(reference);
            roll.setWeightStatus(WeightStatus.MEASURED.name());
            roll.setWeightSource("MANUAL_CONFIRM");
            return true;
        }
        if (dto.getWeightEntryMode() == WeightEntryMode.USER_ESTIMATE) {
            roll.setActualWeight(dto.getActualWeight());
            roll.setWeightStatus(WeightStatus.ESTIMATED.name());
            roll.setWeightSource("MANUAL_ESTIMATE");
            clearMeasurementAudit(roll);
            return false;
        }
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
        if ((dto.getWeightEntryMode() == WeightEntryMode.USER_ESTIMATE
                || dto.getWeightEntryMode() == WeightEntryMode.MEASURED
                || dto.getWeightEntryMode() == WeightEntryMode.CONFIRM_REFERENCE)
                && !positive(dto.getActualWeight())) {
            throw weightError(roll);
        }
        if (weightRequired && !positive(dto.getActualWeight())
                && dto.getWeightEntryMode() != WeightEntryMode.CARRY_NOMINAL) {
            throw weightError(roll);
        }
        if (dto.getActualWeight() != null && !positive(dto.getActualWeight())
                && dto.getWeightEntryMode() != WeightEntryMode.CARRY_NOMINAL) {
            throw weightError(roll);
        }
        if (dto.getWeightEntryMode() == WeightEntryMode.CARRY_NOMINAL && !positive(nominalWeight(roll))) {
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

    private static BigDecimal nominalWeight(OriginalRoll roll) {
        if (roll == null) return null;
        if (WeightStatus.UNKNOWN.name().equalsIgnoreCase(roll.getWeightStatus())) return null;
        if (positive(roll.getTotalWeight())) return roll.getTotalWeight();
        if (!positive(roll.getRollWeight())) return null;
        int pieces = roll.getPieceNum() == null || roll.getPieceNum() < 1 ? 1 : roll.getPieceNum();
        return roll.getRollWeight().multiply(BigDecimal.valueOf(pieces));
    }

    private static BigDecimal referenceWeight(OriginalRoll roll) {
        if (roll == null) return null;
        if (WeightStatus.ESTIMATED.name().equalsIgnoreCase(roll.getWeightStatus())
                && positive(roll.getActualWeight())) {
            return roll.getActualWeight();
        }
        return nominalWeight(roll);
    }

    private static void clearMeasurementAudit(OriginalRoll roll) {
        roll.setWeightRecordedAt(null);
        roll.setWeightRecordedBy(null);
    }

    private static BusinessException weightError(OriginalRoll roll) {
        return new BusinessException("原纸复称实际重量必须大于0：" + identity(roll));
    }

    private static String identity(OriginalRoll roll) {
        return roll.getRollNo() == null || roll.getRollNo().isBlank() ? roll.getUuid() : roll.getRollNo();
    }
}
