package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RewindWidthDifferenceCalculator {

    private static final String ITEM_TRIM = "TRIM";
    private static final int SCALE = 3;

    private RewindWidthDifferenceCalculator() {
    }

    public static Decision calculate(String policyValue, Integer rewindMode, int sourceWidth,
                                     BigDecimal totalWeight, BigDecimal segmentRatio,
                                     RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        return calculate(policyValue, rewindMode, sourceWidth, totalWeight, segmentRatio, segment, 1);
    }

    public static Decision calculate(String policyValue, Integer rewindMode, int sourceWidth,
                                     BigDecimal totalWeight, BigDecimal segmentRatio,
                                     RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                     int sourcePieceCount) {
        if (sourcePieceCount < 1) {
            throw new BusinessException("母卷件数必须大于0");
        }
        if (!supportsWidthPolicy(rewindMode)) {
            requireNoTrim(segment);
            return Decision.disabled();
        }
        WidthDifferencePolicy policy = WidthDifferencePolicy.resolve(policyValue);
        int finishWidth = layoutWidth(segment, false);
        int trimWidth = layoutWidth(segment, true);
        int usedWidth = finishWidth + trimWidth;
        if (sourceWidth <= 0) throw new BusinessException("母卷有效门幅必须大于0");
        if (usedWidth > sourceWidth) throw new BusinessException("复卷成品门幅加切边不能超过母卷门幅");
        int differenceWidth = sourceWidth - usedWidth;
        if (policy == WidthDifferencePolicy.REMAINDER && differenceWidth > 0) {
            throw new BusinessException("留余料模式还有 " + differenceWidth
                    + "mm 未分配，请补齐余料后再保存");
        }
        BigDecimal segmentWeight = totalWeight.multiply(segmentRatio);
        BigDecimal trimWeight = proportional(segmentWeight, trimWidth, sourceWidth);
        BigDecimal differenceWeight = proportional(segmentWeight, differenceWidth, sourceWidth);
        int repeatCount = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
        int finishCount = layoutCount(segment, false) * repeatCount * sourcePieceCount;
        int trimCount = layoutCount(segment, true) * repeatCount * sourcePieceCount;
        int outputCount = finishCount + trimCount;
        BigDecimal allocationShare = policy == WidthDifferencePolicy.ALLOCATE && outputCount > 0
                ? differenceWeight.divide(BigDecimal.valueOf(outputCount), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(SCALE);
        BigDecimal trimAllocation = allocationShare.multiply(BigDecimal.valueOf(trimCount));
        BigDecimal allocationRemainder = policy == WidthDifferencePolicy.ALLOCATE
                ? differenceWeight.subtract(allocationShare.multiply(BigDecimal.valueOf(outputCount)))
                : BigDecimal.ZERO.setScale(SCALE);
        BigDecimal lossWeight = policy == WidthDifferencePolicy.LOSS
                ? differenceWeight : BigDecimal.ZERO.setScale(SCALE);
        return new Decision(policy, trimWidth, trimCount, differenceWidth, differenceWeight, lossWeight,
                trimWeight.add(trimAllocation), allocationShare, allocationRemainder);
    }

    public static boolean supportsWidthPolicy(Integer rewindMode) {
        return rewindMode != null && (rewindMode == 1 || rewindMode == 3
                || rewindMode == 4 || rewindMode == 5);
    }

    private static void requireNoTrim(RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        if (layoutWidth(segment, true) > 0) {
            throw new BusinessException("当前复卷模式不允许配置切边");
        }
    }

    private static int layoutWidth(RewindPlanPreviewDTO.RewindSegmentDTO segment, boolean trim) {
        if (segment.getLayoutItems() == null) return 0;
        return segment.getLayoutItems().stream()
                .filter(item -> isTrim(item) == trim)
                .mapToInt(item -> item.getWidth() * quantity(item)).sum();
    }

    private static int layoutCount(RewindPlanPreviewDTO.RewindSegmentDTO segment, boolean trim) {
        if (segment.getLayoutItems() == null) return 0;
        return segment.getLayoutItems().stream()
                .filter(item -> isTrim(item) == trim)
                .mapToInt(RewindWidthDifferenceCalculator::quantity).sum();
    }

    private static boolean isTrim(RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        return ITEM_TRIM.equalsIgnoreCase(item.getItemType());
    }

    private static int quantity(RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        return item.getQuantity() == null ? 1 : item.getQuantity();
    }

    private static BigDecimal proportional(BigDecimal weight, int width, int sourceWidth) {
        if (width <= 0) return BigDecimal.ZERO.setScale(SCALE);
        return weight.multiply(BigDecimal.valueOf(width))
                .divide(BigDecimal.valueOf(sourceWidth), SCALE, RoundingMode.HALF_UP);
    }

    public record Decision(WidthDifferencePolicy policy, int trimWidth, int trimCount, int differenceWidth,
                           BigDecimal differenceWeight, BigDecimal lossWeight,
                           BigDecimal trimWeight, BigDecimal allocationShare,
                           BigDecimal allocationRemainder) {
        private static Decision disabled() {
            BigDecimal zero = BigDecimal.ZERO.setScale(SCALE);
            return new Decision(null, 0, 0, 0, zero, zero, zero, zero, zero);
        }
    }
}
