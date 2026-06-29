package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiSourceConsumptionNormalizer {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private MultiSourceConsumptionNormalizer() {
    }

    public static boolean hasConsumption(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        if (segments == null) {
            return false;
        }
        return segments.stream()
                .flatMap(segment -> safeSources(segment).stream())
                .anyMatch(source -> source.getConsumeRatio() != null);
    }

    public static void normalize(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments,
                                 Map<String, OriginalRoll> rollByUuid) {
        if (!hasConsumption(segments)) {
            return;
        }
        validateCompleteConsumption(segments);
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            normalizeSegment(segment, rollByUuid);
        }
    }

    public static BigDecimal totalConsumedWeight(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments,
                                                 Map<String, OriginalRoll> rollByUuid) {
        BigDecimal total = BigDecimal.ZERO;
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            total = total.add(segmentConsumedWeight(segment, rollByUuid));
        }
        return total.setScale(3, RoundingMode.HALF_UP);
    }

    public static BigDecimal segmentConsumedWeight(RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                                   Map<String, OriginalRoll> rollByUuid) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinishConfigSpecDTO.FinishSourceDTO source : safeSources(segment)) {
            total = total.add(sourceConsumedWeight(source, rollByUuid));
        }
        return total;
    }

    private static void normalizeSegment(RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                         Map<String, OriginalRoll> rollByUuid) {
        List<FinishConfigSpecDTO.FinishSourceDTO> sources = safeSources(segment);
        BigDecimal segmentWeight = segmentConsumedWeight(segment, rollByUuid);
        if (segmentWeight.signum() <= 0) {
            throw new BusinessException("多母卷合并复卷每个分段必须填写来源消耗比例");
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < sources.size(); i++) {
            FinishConfigSpecDTO.FinishSourceDTO source = sources.get(i);
            BigDecimal share = i == sources.size() - 1
                    ? HUNDRED.subtract(allocated)
                    : sourceConsumedWeight(source, rollByUuid).multiply(HUNDRED).divide(segmentWeight, 2, RoundingMode.HALF_UP);
            source.setShareRatio(share.setScale(2, RoundingMode.HALF_UP));
            allocated = allocated.add(source.getShareRatio());
        }
    }

    private static void validateCompleteConsumption(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        Map<String, BigDecimal> totals = consumptionTotals(segments);
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            BigDecimal diff = entry.getValue().subtract(HUNDRED).abs();
            if (diff.compareTo(TOLERANCE) > 0) {
                throw new BusinessException("来源母卷 " + entry.getKey()
                        + " 消耗比例合计为 " + entry.getValue().stripTrailingZeros().toPlainString()
                        + "%，必须等于100%");
            }
        }
    }

    private static Map<String, BigDecimal> consumptionTotals(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            for (FinishConfigSpecDTO.FinishSourceDTO source : safeSources(segment)) {
                BigDecimal ratio = source.getConsumeRatio();
                if (ratio == null || ratio.signum() <= 0) {
                    throw new BusinessException("多母卷合并复卷必须填写来源消耗比例");
                }
                totals.merge(source.getOriginalUuid(), ratio, BigDecimal::add);
            }
        }
        return totals;
    }

    private static BigDecimal sourceConsumedWeight(FinishConfigSpecDTO.FinishSourceDTO source,
                                                   Map<String, OriginalRoll> rollByUuid) {
        OriginalRoll roll = rollByUuid.get(source.getOriginalUuid());
        if (roll == null || source.getConsumeRatio() == null) {
            return BigDecimal.ZERO;
        }
        return totalWeight(roll).multiply(source.getConsumeRatio()).divide(HUNDRED, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal totalWeight(OriginalRoll roll) {
        BigDecimal weight = roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight();
        int pieceNum = roll.getPieceNum() == null ? 1 : roll.getPieceNum();
        return weight.multiply(BigDecimal.valueOf(pieceNum));
    }

    private static List<FinishConfigSpecDTO.FinishSourceDTO> safeSources(RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        return segment.getSources() == null ? List.of() : segment.getSources();
    }
}
