package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQuery;
import org.springframework.util.StringUtils;

import java.util.Set;

public final class ReportDimensionPolicy {
    private static final Set<String> DIMENSIONS = Set.of(
            "month", "customer", "paper", "process", "machine", "invoice", "settleType", "status");

    private ReportDimensionPolicy() {
    }

    public static String dimensionOf(ReportQuery query) {
        String dimension = query == null ? null : query.getDimension();
        if (!StringUtils.hasText(dimension)) return "customer";
        if (!DIMENSIONS.contains(dimension)) {
            throw new BusinessException("不支持的统计维度：" + dimension);
        }
        return dimension;
    }
}
