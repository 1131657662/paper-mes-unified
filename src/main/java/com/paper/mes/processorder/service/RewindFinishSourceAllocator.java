package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Allocates segment-level source consumption across its persisted finishes. */
public final class RewindFinishSourceAllocator {

    private static final int RATIO_SCALE = 2;

    private RewindFinishSourceAllocator() {
    }

    public static List<List<FinishConfigSpecDTO.FinishSourceDTO>> allocate(
            List<FinishPreviewVO.FinishItemPreview> finishes,
            List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        List<List<FinishConfigSpecDTO.FinishSourceDTO>> result = emptyResult(finishes.size());
        Map<Integer, List<Integer>> indexesBySegment = groupFinishIndexes(finishes);
        Map<Integer, List<FinishConfigSpecDTO.FinishSourceDTO>> sourcesBySegment = segmentSources(segments);
        indexesBySegment.forEach((segmentSort, indexes) -> allocateSegment(
                finishes, indexes, sourcesBySegment.getOrDefault(segmentSort, List.of()), result));
        return result;
    }

    private static void allocateSegment(List<FinishPreviewVO.FinishItemPreview> finishes,
                                        List<Integer> indexes,
                                        List<FinishConfigSpecDTO.FinishSourceDTO> sources,
                                        List<List<FinishConfigSpecDTO.FinishSourceDTO>> result) {
        if (sources.isEmpty()) return;
        List<BigDecimal> bases = indexes.stream()
                .map(index -> positiveOrZero(finishes.get(index).getEstimateWeight())).toList();
        BigDecimal totalBasis = bases.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (FinishConfigSpecDTO.FinishSourceDTO source : sources) {
            List<BigDecimal> allocations = allocateRatio(source.getConsumeRatio(), bases, totalBasis);
            for (int i = 0; i < indexes.size(); i++) {
                result.get(indexes.get(i)).add(copy(source, allocations.get(i)));
            }
        }
    }

    private static List<BigDecimal> allocateRatio(BigDecimal target, List<BigDecimal> bases,
                                                   BigDecimal totalBasis) {
        if (bases.isEmpty()) return List.of();
        if (target == null) return java.util.Collections.nCopies(bases.size(), null);
        BigDecimal roundedTarget = target.setScale(RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal cumulativeBasis = BigDecimal.ZERO;
        List<BigDecimal> result = new ArrayList<>(bases.size());
        for (int i = 0; i < bases.size(); i++) {
            cumulativeBasis = cumulativeBasis.add(bases.get(i));
            BigDecimal cumulative = i == bases.size() - 1
                    ? roundedTarget
                    : cumulativeShare(target, cumulativeBasis, totalBasis, i + 1, bases.size());
            BigDecimal value = cumulative.subtract(allocated);
            result.add(value);
            allocated = cumulative;
        }
        return result;
    }

    private static BigDecimal cumulativeShare(BigDecimal target, BigDecimal cumulativeBasis,
                                              BigDecimal totalBasis, int position, int count) {
        if (totalBasis.signum() > 0) {
            return target.multiply(cumulativeBasis)
                    .divide(totalBasis, RATIO_SCALE, RoundingMode.HALF_UP);
        }
        return target.multiply(BigDecimal.valueOf(position))
                .divide(BigDecimal.valueOf(count), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static FinishConfigSpecDTO.FinishSourceDTO copy(
            FinishConfigSpecDTO.FinishSourceDTO source, BigDecimal consumeRatio) {
        FinishConfigSpecDTO.FinishSourceDTO copy = new FinishConfigSpecDTO.FinishSourceDTO();
        copy.setOriginalUuid(source.getOriginalUuid());
        copy.setShareRatio(source.getShareRatio());
        copy.setConsumeRatio(consumeRatio);
        return copy;
    }

    private static Map<Integer, List<Integer>> groupFinishIndexes(
            List<FinishPreviewVO.FinishItemPreview> finishes) {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        for (int i = 0; i < finishes.size(); i++) {
            result.computeIfAbsent(finishes.get(i).getSegmentSort(), key -> new ArrayList<>()).add(i);
        }
        return result;
    }

    private static Map<Integer, List<FinishConfigSpecDTO.FinishSourceDTO>> segmentSources(
            List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        Map<Integer, List<FinishConfigSpecDTO.FinishSourceDTO>> result = new LinkedHashMap<>();
        if (segments == null) return result;
        for (int i = 0; i < segments.size(); i++) {
            RewindPlanPreviewDTO.RewindSegmentDTO segment = segments.get(i);
            int sort = segment.getSegmentSort() == null ? i + 1 : segment.getSegmentSort();
            result.put(sort, segment.getSources() == null ? List.of() : segment.getSources());
        }
        return result;
    }

    private static List<List<FinishConfigSpecDTO.FinishSourceDTO>> emptyResult(int size) {
        List<List<FinishConfigSpecDTO.FinishSourceDTO>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(new ArrayList<>());
        return result;
    }

    private static BigDecimal positiveOrZero(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : BigDecimal.ZERO;
    }
}
