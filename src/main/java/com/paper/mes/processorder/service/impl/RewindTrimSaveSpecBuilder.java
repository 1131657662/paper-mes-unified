package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.service.RewindWidthDifferenceCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class RewindTrimSaveSpecBuilder {

    private static final String ITEM_TRIM = "TRIM";
    private static final int SCALE = 3;

    private RewindTrimSaveSpecBuilder() {
    }

    static List<FinishConfigSpecDTO> build(FinishPreviewVO preview, FinishConfigSaveDTO dto) {
        return build(preview, dto, 1);
    }

    static List<FinishConfigSpecDTO> build(FinishPreviewVO preview, FinishConfigSaveDTO dto,
                                           int sourcePieceCount) {
        if (sourcePieceCount < 1) {
            throw new BusinessException("母卷件数必须大于0");
        }
        List<FinishPreviewVO.SegmentPreview> previews = preview.getSegments();
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = dto.getRewindSegments();
        if (previews == null || previews.isEmpty()) return List.of();
        if (segments == null || segments.size() != previews.size()) {
            throw new BusinessException("复卷分段预览与保存方案不一致");
        }
        BigDecimal totalWeight = totalWeight(preview);
        int sourceWidth = preview.getOriginalWidth() == null ? 0 : preview.getOriginalWidth();
        List<FinishConfigSpecDTO> result = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            result.addAll(buildSegment(dto, segments.get(index), previews.get(index), totalWeight, sourceWidth,
                    sourcePieceCount));
        }
        return result;
    }

    private static List<FinishConfigSpecDTO> buildSegment(
            FinishConfigSaveDTO dto, RewindPlanPreviewDTO.RewindSegmentDTO segment,
            FinishPreviewVO.SegmentPreview preview, BigDecimal totalWeight, int sourceWidth,
            int sourcePieceCount) {
        List<Integer> widths = expandedTrimWidths(segment, sourcePieceCount);
        if (widths.isEmpty()) return List.of();
        BigDecimal ratio = preview.getSegmentRatio() == null ? BigDecimal.ZERO : preview.getSegmentRatio();
        RewindWidthDifferenceCalculator.Decision decision = RewindWidthDifferenceCalculator.calculate(
                dto.getWidthDifferencePolicy(), dto.getRewindMode(), sourceWidth, totalWeight, ratio, segment,
                sourcePieceCount);
        List<BigDecimal> weights = trimWeights(widths, totalWeight, ratio, sourceWidth, segment, decision,
                sourcePieceCount);
        List<FinishConfigSpecDTO> result = new ArrayList<>(widths.size());
        for (int index = 0; index < widths.size(); index++) {
            result.add(trimSpec(widths.get(index), weights.get(index), dto, segment));
        }
        return result;
    }

    private static List<Integer> expandedTrimWidths(RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                                    int sourcePieceCount) {
        List<Integer> result = new ArrayList<>();
        int repeatCount = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
        for (int sourcePiece = 0; sourcePiece < sourcePieceCount; sourcePiece++) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : segment.getLayoutItems()) {
                    if (!ITEM_TRIM.equalsIgnoreCase(item.getItemType())) continue;
                    int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
                    for (int count = 0; count < quantity; count++) result.add(item.getWidth());
                }
            }
        }
        return result;
    }

    private static List<BigDecimal> trimWeights(
            List<Integer> widths, BigDecimal totalWeight, BigDecimal segmentRatio, int sourceWidth,
            RewindPlanPreviewDTO.RewindSegmentDTO segment,
            RewindWidthDifferenceCalculator.Decision decision, int sourcePieceCount) {
        int repeatCount = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
        BigDecimal repeatRatio = segmentRatio.divide(BigDecimal.valueOf(repeatCount * (long) sourcePieceCount),
                6, RoundingMode.HALF_UP);
        List<BigDecimal> result = new ArrayList<>(widths.size());
        BigDecimal targetWeight = decision.trimWeight().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal repeatWeight = totalWeight.multiply(repeatRatio);
        BigDecimal rawCumulative = BigDecimal.ZERO;
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < widths.size(); index++) {
            rawCumulative = rawCumulative.add(proportional(repeatWeight, widths.get(index), sourceWidth))
                    .add(decision.allocationShare());
            BigDecimal roundedCumulative = rawCumulative.setScale(SCALE, RoundingMode.HALF_UP)
                    .min(targetWeight);
            BigDecimal weight = index == widths.size() - 1
                    ? targetWeight.subtract(allocated)
                    : roundedCumulative.subtract(allocated);
            result.add(weight.setScale(SCALE, RoundingMode.HALF_UP));
            allocated = allocated.add(weight);
        }
        return result;
    }

    private static FinishConfigSpecDTO trimSpec(
            int width, BigDecimal weight, FinishConfigSaveDTO dto,
            RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(ITEM_TRIM);
        spec.setCount(1);
        spec.setFinishWidth(width);
        spec.setEstimateWeight(weight);
        if (Integer.valueOf(5).equals(dto.getRewindMode())) spec.setSources(segment.getSources());
        return spec;
    }

    private static BigDecimal proportional(BigDecimal weight, int width, int sourceWidth) {
        if (sourceWidth <= 0) return BigDecimal.ZERO.setScale(SCALE + 6);
        return weight.multiply(BigDecimal.valueOf(width))
                .divide(BigDecimal.valueOf(sourceWidth), SCALE + 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal totalWeight(FinishPreviewVO preview) {
        return value(preview.getTotalEstimateWeight()).add(value(preview.getTotalTrimWeight()))
                .add(value(preview.getCalculatedLossWeight())).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
