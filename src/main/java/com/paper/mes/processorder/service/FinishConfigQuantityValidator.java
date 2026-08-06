package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;

import java.util.List;

public final class FinishConfigQuantityValidator {

    public static final int MAX_TOTAL_FINISHES = 500;
    public static final int MAX_SOURCE_PIECES = 500;
    private static final int STEP_TYPE_REWIND = 2;

    private FinishConfigQuantityValidator() {
    }

    public static void requireWithinLimit(FinishConfigSaveDTO config) {
        requireWithinLimit(config, 1);
    }

    public static void requireWithinLimit(FinishConfigSaveDTO config, int sourcePieceCount) {
        requireSourcePieceCount(sourcePieceCount);
        long specificationCount = specCount(config.getFinishSpecs());
        boolean useSegments = !safe(config.getRewindSegments()).isEmpty()
                && (Integer.valueOf(STEP_TYPE_REWIND).equals(config.getMainStepType())
                || specificationCount == 0);
        long total = useSegments ? previewSegmentCount(config.getRewindSegments()) : specificationCount;
        if (requiresSourceExpansion(config.getProcessMode(), config.getMainStepType(), config.getRewindMode())) {
            total = multiply(total, sourcePieceCount);
        }
        total += config.getSpareCount() == null ? 0 : config.getSpareCount();
        requireTotal(total);
    }

    public static void requireWithinLimit(ProcessPlanDTO plan) {
        requireWithinLimit(plan, 1);
    }

    public static void requireWithinLimit(ProcessPlanDTO plan, int sourcePieceCount) {
        requireSourcePieceCount(sourcePieceCount);
        long specificationCount = specCount(plan.getFinishSpecs());
        boolean useSegments = !safe(plan.getSegments()).isEmpty()
                && (Integer.valueOf(STEP_TYPE_REWIND).equals(plan.getMainStepType())
                || specificationCount == 0);
        long total = useSegments ? planSegmentCount(plan.getSegments()) : specificationCount;
        if (requiresSourceExpansion(plan.getProcessMode(), plan.getMainStepType(), plan.getRewindMode())) {
            total = multiply(total, sourcePieceCount);
        }
        total += plan.getSpareCount() == null ? 0 : plan.getSpareCount();
        requireTotal(total);
    }

    public static void requireWithinLimit(RewindPlanPreviewDTO preview) {
        requireWithinLimit(preview, 1);
    }

    public static void requireWithinLimit(RewindPlanPreviewDTO preview, int sourcePieceCount) {
        requireSourcePieceCount(sourcePieceCount);
        long total = previewSegmentCount(preview.getSegments());
        if (!Integer.valueOf(5).equals(preview.getRewindMode())) {
            total = multiply(total, sourcePieceCount);
        }
        total += preview.getSpareCount() == null ? 0 : preview.getSpareCount();
        requireTotal(total);
    }

    public static void requireSourcePieceCount(Integer sourcePieceCount) {
        int count = sourcePieceCount == null ? 1 : sourcePieceCount;
        if (count < 1 || count > MAX_SOURCE_PIECES) {
            throw new BusinessException("母卷件数必须在1到500之间");
        }
    }

    private static boolean requiresSourceExpansion(Integer processMode, Integer mainStepType, Integer rewindMode) {
        if (Integer.valueOf(2).equals(processMode)
                || Integer.valueOf(3).equals(processMode)
                || Integer.valueOf(4).equals(processMode)) {
            return false;
        }
        if (Integer.valueOf(2).equals(mainStepType)) {
            return !Integer.valueOf(5).equals(rewindMode);
        }
        return Integer.valueOf(1).equals(mainStepType);
    }

    private static void requireTotal(long total) {
        if (total > MAX_TOTAL_FINISHES) {
            throw new BusinessException("单个母卷展开后的成品、余料和备用号总数不能超过500");
        }
    }

    private static long specCount(List<FinishConfigSpecDTO> specs) {
        return safe(specs).stream().map(FinishConfigSpecDTO::getCount)
                .filter(count -> count != null && count > 0)
                .mapToLong(Integer::longValue).sum();
    }

    private static long previewSegmentCount(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        long total = 0;
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : safe(segments)) {
            long quantity = safe(segment.getLayoutItems()).stream()
                    .mapToLong(item -> positiveOrDefault(item.getQuantity())).sum();
            total = addExpandedCount(total, segment.getRepeatCount(), quantity);
            if (total > MAX_TOTAL_FINISHES) return total;
        }
        return total;
    }

    private static long planSegmentCount(List<RewindSegmentPlanDTO> segments) {
        return safe(segments).stream().mapToLong(segment -> multiply(
                segment.getRepeatCount(), safe(segment.getLayoutItems()).stream()
                        .mapToLong(item -> positiveOrDefault(item.getQuantity())).sum())).sum();
    }

    private static long multiply(Integer repeatCount, long quantity) {
        long repeat = positiveOrDefault(repeatCount);
        if (quantity == 0) return 0;
        if (repeat > MAX_TOTAL_FINISHES || quantity > MAX_TOTAL_FINISHES) return MAX_TOTAL_FINISHES + 1L;
        return Math.min(repeat * quantity, MAX_TOTAL_FINISHES + 1L);
    }

    private static long multiply(long quantity, int multiplier) {
        if (quantity == 0 || multiplier <= 0) return 0;
        if (quantity > MAX_TOTAL_FINISHES || multiplier > MAX_TOTAL_FINISHES
                || quantity > MAX_TOTAL_FINISHES / multiplier) {
            return MAX_TOTAL_FINISHES + 1L;
        }
        return quantity * multiplier;
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

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
