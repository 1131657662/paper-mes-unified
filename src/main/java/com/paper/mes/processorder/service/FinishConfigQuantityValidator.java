package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;

import java.util.List;

public final class FinishConfigQuantityValidator {

    public static final int MAX_TOTAL_FINISHES = 500;

    private FinishConfigQuantityValidator() {
    }

    public static void requireWithinLimit(FinishConfigSaveDTO config) {
        long total = specCount(config.getFinishSpecs());
        if (total == 0) {
            total = previewSegmentCount(config.getRewindSegments());
        }
        total += config.getSpareCount() == null ? 0 : config.getSpareCount();
        requireTotal(total);
    }

    public static void requireWithinLimit(ProcessPlanDTO plan) {
        long total = specCount(plan.getFinishSpecs());
        if (total == 0) {
            total = planSegmentCount(plan.getSegments());
        }
        total += plan.getSpareCount() == null ? 0 : plan.getSpareCount();
        requireTotal(total);
    }

    public static void requireWithinLimit(RewindPlanPreviewDTO preview) {
        long total = previewSegmentCount(preview.getSegments());
        total += preview.getSpareCount() == null ? 0 : preview.getSpareCount();
        requireTotal(total);
    }

    private static void requireTotal(long total) {
        if (total > MAX_TOTAL_FINISHES) {
            throw new BusinessException("单个母卷展开后的成品和备用号总数不能超过500");
        }
    }

    private static long specCount(List<FinishConfigSpecDTO> specs) {
        return safe(specs).stream().filter(FinishConfigQuantityValidator::isFinish)
                .map(FinishConfigSpecDTO::getCount)
                .filter(count -> count != null && count > 0)
                .mapToLong(Integer::longValue).sum();
    }

    private static long previewSegmentCount(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        long total = 0;
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : safe(segments)) {
            long quantity = safe(segment.getLayoutItems()).stream()
                    .filter(FinishConfigQuantityValidator::isFinish)
                    .mapToLong(item -> positiveOrDefault(item.getQuantity())).sum();
            total = addExpandedCount(total, segment.getRepeatCount(), quantity);
            if (total > MAX_TOTAL_FINISHES) return total;
        }
        return total;
    }

    private static long planSegmentCount(List<RewindSegmentPlanDTO> segments) {
        return safe(segments).stream().mapToLong(segment -> multiply(
                segment.getRepeatCount(), safe(segment.getLayoutItems()).stream()
                        .filter(FinishConfigQuantityValidator::isFinish)
                        .mapToLong(item -> positiveOrDefault(item.getQuantity())).sum())).sum();
    }

    private static long multiply(Integer repeatCount, long quantity) {
        long repeat = positiveOrDefault(repeatCount);
        if (quantity == 0) return 0;
        if (repeat > MAX_TOTAL_FINISHES || quantity > MAX_TOTAL_FINISHES) return MAX_TOTAL_FINISHES + 1L;
        return Math.min(repeat * quantity, MAX_TOTAL_FINISHES + 1L);
    }

    private static long addExpandedCount(long total, Integer repeatCount, long quantity) {
        long repeat = positiveOrDefault(repeatCount);
        if (quantity == 0) return total;
        if (repeat > MAX_TOTAL_FINISHES || quantity > MAX_TOTAL_FINISHES) return MAX_TOTAL_FINISHES + 1L;
        long expanded = repeat * quantity;
        return total > MAX_TOTAL_FINISHES - expanded ? MAX_TOTAL_FINISHES + 1L : total + expanded;
    }

    private static long positiveOrDefault(Integer value) {
        long resolved = value == null ? 1L : value.longValue();
        if (resolved < 1) {
            throw new BusinessException("复卷展开数量必须大于0");
        }
        return resolved;
    }

    private static boolean isFinish(FinishConfigSpecDTO spec) {
        return spec.getItemType() == null || !"TRIM".equalsIgnoreCase(spec.getItemType());
    }

    private static boolean isFinish(RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        return item.getItemType() == null || !"TRIM".equalsIgnoreCase(item.getItemType());
    }

    private static boolean isFinish(RewindLayoutItemPlanDTO item) {
        return item.getItemType() == null || !"TRIM".equalsIgnoreCase(item.getItemType());
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
