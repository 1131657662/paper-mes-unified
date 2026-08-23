package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.service.RewindFinishSourceAllocator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class RewindTrimSaveSpecBuilder {

    private static final String ITEM_TRIM = "TRIM";

    private RewindTrimSaveSpecBuilder() {
    }

    static List<FinishConfigSpecDTO> build(FinishPreviewVO preview, FinishConfigSaveDTO dto) {
        return build(preview, dto, 1);
    }

    static List<FinishConfigSpecDTO> build(FinishPreviewVO preview, FinishConfigSaveDTO dto,
                                           int sourcePieceCount) {
        return buildResult(preview, dto, sourcePieceCount).specs();
    }

    static TrimBuildResult buildResult(FinishPreviewVO preview, FinishConfigSaveDTO dto,
                                       int sourcePieceCount) {
        if (sourcePieceCount < 1) throw new BusinessException("母卷件数必须大于0");
        List<FinishPreviewVO.SegmentPreview> previews = preview.getSegments();
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = dto.getRewindSegments();
        if (previews == null || previews.isEmpty()) return new TrimBuildResult(List.of(), List.of());
        if (segments == null || segments.size() != previews.size()) {
            throw new BusinessException("复卷分段预览与保存方案不一致");
        }
        List<TrimCandidate> candidates = trimCandidates(previews, segments, sourcePieceCount);
        List<BigDecimal> weights = IntegerWeightAllocator.allocate(
                IntegerWeightAllocator.roundTotal(preview.getTotalTrimWeight()),
                candidates.stream().map(TrimCandidate::basis).toList());
        List<RewindFinishSourceAllocator.WeightedOutput> outputs = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            outputs.add(new RewindFinishSourceAllocator.WeightedOutput(
                    candidates.get(index).segmentSort(), preview.isWeightPending() ? null : weights.get(index)));
        }
        return new TrimBuildResult(trimSpecs(candidates, weights, dto, preview.isWeightPending()), outputs);
    }

    private static List<TrimCandidate> trimCandidates(
            List<FinishPreviewVO.SegmentPreview> previews,
            List<RewindPlanPreviewDTO.RewindSegmentDTO> segments,
            int sourcePieceCount) {
        List<TrimCandidate> result = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            RewindPlanPreviewDTO.RewindSegmentDTO segment = segments.get(index);
            BigDecimal ratio = value(previews.get(index).getSegmentRatio());
            int repeats = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
            BigDecimal perRepeatRatio = ratio.divide(
                    BigDecimal.valueOf(repeats * (long) sourcePieceCount), 12,
                    java.math.RoundingMode.HALF_UP);
            int segmentSort = segment.getSegmentSort() == null ? index + 1 : segment.getSegmentSort();
            appendCandidates(result, segment, sourcePieceCount, perRepeatRatio, segmentSort);
        }
        return result;
    }

    private static void appendCandidates(List<TrimCandidate> target,
                                         RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                         int sourcePieceCount, BigDecimal ratio, int segmentSort) {
        int repeats = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
        List<RewindPlanPreviewDTO.RewindLayoutItemDTO> items = segment.getLayoutItems() == null
                ? List.of() : segment.getLayoutItems();
        for (int source = 0; source < sourcePieceCount; source++) {
            for (int repeat = 0; repeat < repeats; repeat++) {
                for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : items) {
                    if (!ITEM_TRIM.equalsIgnoreCase(item.getItemType())) continue;
                    int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
                    for (int count = 0; count < quantity; count++) {
                        target.add(new TrimCandidate(segmentSort, item.getWidth(),
                                BigDecimal.valueOf(item.getWidth()).multiply(ratio), segment.getSources()));
                    }
                }
            }
        }
    }

    private static List<FinishConfigSpecDTO> trimSpecs(List<TrimCandidate> candidates,
                                                        List<BigDecimal> weights,
                                                        FinishConfigSaveDTO dto,
                                                        boolean weightPending) {
        List<FinishConfigSpecDTO> result = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            TrimCandidate candidate = candidates.get(index);
            FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
            spec.setItemType(ITEM_TRIM);
            spec.setCount(1);
            spec.setFinishWidth(candidate.width());
            spec.setEstimateWeight(weightPending ? null : weights.get(index));
            result.add(spec);
        }
        return result;
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record TrimCandidate(int segmentSort, int width, BigDecimal basis,
                                 List<FinishConfigSpecDTO.FinishSourceDTO> sources) {
    }

    record TrimBuildResult(List<FinishConfigSpecDTO> specs,
                           List<RewindFinishSourceAllocator.WeightedOutput> outputs) {
    }
}
