package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.model.WeightStatus;

import java.math.BigDecimal;

/** Keeps entry-point weight semantics consistent before a roll reaches production. */
public final class OriginalRollWeightPolicy {

    private OriginalRollWeightPolicy() {
    }

    public static String normalizeEntryStatus(WeightStatus requested, BigDecimal weight) {
        if (requested == WeightStatus.MEASURED) {
            throw measuredEntryError();
        }
        if (requested == WeightStatus.UNKNOWN) {
            return WeightStatus.UNKNOWN.name();
        }
        if (requested == WeightStatus.ESTIMATED) {
            requirePositiveEstimatedWeight(weight);
            return WeightStatus.ESTIMATED.name();
        }
        return isPositive(weight) ? WeightStatus.ESTIMATED.name() : WeightStatus.UNKNOWN.name();
    }

    public static String normalizeEntryStatus(String requested, BigDecimal weight) {
        if (requested == null || requested.isBlank()) {
            return normalizeEntryStatus((WeightStatus) null, weight);
        }
        final WeightStatus parsed;
        try {
            parsed = WeightStatus.valueOf(requested.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("母卷重量状态无效，仅支持 UNKNOWN 或 ESTIMATED");
        }
        return normalizeEntryStatus(parsed, weight);
    }

    private static void requirePositiveEstimatedWeight(BigDecimal weight) {
        if (!isPositive(weight)) {
            throw new BusinessException("标称/估算重量必须大于0；未知重量请选择 UNKNOWN");
        }
    }

    private static boolean isPositive(BigDecimal weight) {
        return weight != null && weight.signum() > 0;
    }

    private static BusinessException measuredEntryError() {
        return new BusinessException("新建或编辑母卷不能直接标记为 MEASURED，请在回录工作台录入实测重量");
    }
}
