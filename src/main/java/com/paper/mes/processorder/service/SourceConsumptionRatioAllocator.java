package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves persisted legacy empty ratios against explicit source consumption. */
public final class SourceConsumptionRatioAllocator {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private SourceConsumptionRatioAllocator() {
    }

    public static List<BigDecimal> allocate(List<SourceRatio> sources) {
        if (sources == null || sources.isEmpty()) return List.of();
        Map<String, BigDecimal> explicit = explicitTotals(sources);
        validateExplicitTotals(explicit);
        Map<String, BigDecimal> consumed = new LinkedHashMap<>();
        Set<String> legacyAssigned = new HashSet<>();
        List<BigDecimal> result = new ArrayList<>(sources.size());
        for (SourceRatio source : sources) {
            BigDecimal requested = requestedRatio(source, explicit, legacyAssigned);
            BigDecimal remaining = HUNDRED.subtract(
                    consumed.getOrDefault(source.sourceKey(), BigDecimal.ZERO)).max(BigDecimal.ZERO);
            BigDecimal applied = requested.min(remaining);
            result.add(applied);
            consumed.merge(source.sourceKey(), applied, BigDecimal::add);
        }
        return List.copyOf(result);
    }

    private static Map<String, BigDecimal> explicitTotals(List<SourceRatio> sources) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (SourceRatio source : sources) {
            validateSource(source);
            if (source.consumeRatio() != null && source.consumeRatio().signum() > 0) {
                totals.merge(source.sourceKey(), source.consumeRatio(), BigDecimal::add);
            }
        }
        return totals;
    }

    private static BigDecimal requestedRatio(SourceRatio source, Map<String, BigDecimal> explicit,
                                             Set<String> legacyAssigned) {
        if (source.consumeRatio() != null && source.consumeRatio().signum() > 0) {
            return source.consumeRatio();
        }
        if (!legacyAssigned.add(source.sourceKey())) return BigDecimal.ZERO;
        return HUNDRED.subtract(explicit.getOrDefault(source.sourceKey(), BigDecimal.ZERO))
                .max(BigDecimal.ZERO);
    }

    private static void validateSource(SourceRatio source) {
        if (source == null || source.sourceKey() == null || source.sourceKey().isBlank()) {
            throw new BusinessException("来源标识不能为空");
        }
        if (source.consumeRatio() != null && source.consumeRatio().signum() < 0) {
            throw new BusinessException("来源消耗比例不能为负");
        }
    }

    private static void validateExplicitTotals(Map<String, BigDecimal> totals) {
        if (totals.values().stream().anyMatch(value -> value.compareTo(HUNDRED) > 0)) {
            throw new BusinessException("来源母卷消耗比例合计不能超过100%");
        }
    }

    public record SourceRatio(String sourceKey, BigDecimal consumeRatio) {
    }
}
