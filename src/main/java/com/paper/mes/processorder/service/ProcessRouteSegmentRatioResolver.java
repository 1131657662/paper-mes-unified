package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Resolves one canonical source-consumption ratio for each rewind segment. */
final class ProcessRouteSegmentRatioResolver {

    private ProcessRouteSegmentRatioResolver() {
    }

    static Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios(
            List<RewindSegmentPlanDTO> segments) {
        Map<RewindSourcePlanDTO, BigDecimal> result = new IdentityHashMap<>();
        List<RewindSourcePlanDTO> sources = segments.stream()
                .flatMap(segment -> segment.getSources() == null
                        ? java.util.stream.Stream.empty() : segment.getSources().stream())
                .toList();
        List<BigDecimal> ratios = SourceConsumptionRatioAllocator.allocate(sources.stream()
                .map(source -> new SourceConsumptionRatioAllocator.SourceRatio(
                        source.getOriginalUuid(), source.getConsumeRatio())).toList());
        for (int index = 0; index < sources.size(); index++) {
            result.put(sources.get(index), ratios.get(index));
        }
        return result;
    }

    static BigDecimal segmentRatio(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                   RewindSegmentPlanDTO segment,
                                   Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs,
                                   Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios) {
        BigDecimal consumed = BigDecimal.ZERO;
        List<RewindSourcePlanDTO> sources = segment.getSources() == null
                ? List.of() : segment.getSources();
        for (RewindSourcePlanDTO source : sources) {
            ProcessRoutePreviewVO.RouteOutputVO input = inputs.get(source.getOriginalUuid());
            BigDecimal inputWeight = ProcessRoutePreviewValidator.effectiveOutputWeight(input);
            BigDecimal ratio = effectiveRatios.getOrDefault(source, BigDecimal.ZERO);
            if (inputWeight != null) {
                consumed = consumed.add(inputWeight.multiply(ratio).movePointLeft(2));
            }
        }
        if (!sources.isEmpty()) {
            if (consumed.signum() <= 0) return BigDecimal.ZERO;
            BigDecimal total = stage.getInputOutputKeys() == null ? BigDecimal.ZERO
                    : stage.getInputOutputKeys().stream().map(inputs::get)
                    .map(ProcessRoutePreviewValidator::effectiveOutputWeight)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.signum() > 0
                    ? consumed.divide(total, 12, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }
        BigDecimal configured = segment.getSegmentRatio() == null
                ? BigDecimal.ONE : segment.getSegmentRatio();
        BigDecimal configuredTotal = stage.getPlan().getSegments().stream()
                .map(item -> item.getSegmentRatio() == null ? BigDecimal.ONE : item.getSegmentRatio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return configuredTotal.signum() > 0
                ? configured.divide(configuredTotal, 12, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
