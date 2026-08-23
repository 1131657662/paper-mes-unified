package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class RewindFinishSourceAllocatorTest {

    @Test
    void allocate_distributesConsumptionByFinishWeightAndClosesRemainder() {
        List<List<FinishConfigSpecDTO.FinishSourceDTO>> result = RewindFinishSourceAllocator.allocate(
                List.of(finish(1, "100"), finish(1, "200"), finish(1, "100")),
                List.of(segment(1, source("roll-1", "70"))));

        assertEquals(List.of(new BigDecimal("17.50"), new BigDecimal("35.00"), new BigDecimal("17.50")),
                result.stream().map(row -> row.getFirst().getConsumeRatio()).toList());
        assertEquals(new BigDecimal("70.00"), result.stream()
                .map(row -> row.getFirst().getConsumeRatio()).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertNotSame(result.getFirst().getFirst(), result.get(1).getFirst());
    }

    @Test
    void allocate_pendingWeightsDistributesEvenlyAndPreservesExactTotal() {
        List<List<FinishConfigSpecDTO.FinishSourceDTO>> result = RewindFinishSourceAllocator.allocate(
                List.of(finish(1, null), finish(1, null), finish(1, null)),
                List.of(segment(1, source("roll-1", "70"))));

        assertEquals(List.of(new BigDecimal("23.33"), new BigDecimal("23.34"), new BigDecimal("23.33")),
                result.stream().map(row -> row.getFirst().getConsumeRatio()).toList());
    }

    @Test
    void allocate_manyFinishesNeverCreatesNegativeRoundingRemainder() {
        List<FinishPreviewVO.FinishItemPreview> finishes = new ArrayList<>();
        for (int i = 0; i < 100; i++) finishes.add(finish(1, null));

        List<List<FinishConfigSpecDTO.FinishSourceDTO>> result = RewindFinishSourceAllocator.allocate(
                finishes, List.of(segment(1, source("roll-1", "0.50"))));

        List<BigDecimal> ratios = result.stream()
                .map(row -> row.getFirst().getConsumeRatio()).toList();
        assertEquals(new BigDecimal("0.50"), ratios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(0, ratios.stream().filter(value -> value.signum() < 0).count());
    }

    @Test
    void allocateWithExtrasSharesConsumptionAcrossFinishesAndTrim() {
        RewindFinishSourceAllocator.Allocation allocation = RewindFinishSourceAllocator.allocateWithExtras(
                List.of(finish(1, "900")),
                List.of(new RewindFinishSourceAllocator.WeightedOutput(1, new BigDecimal("100"))),
                List.of(segment(1, source("roll-1", "100"))));

        assertEquals(new BigDecimal("90.00"),
                allocation.finishSources().getFirst().getFirst().getConsumeRatio());
        assertEquals(new BigDecimal("10.00"),
                allocation.extraSources().getFirst().getFirst().getConsumeRatio());
    }

    private FinishPreviewVO.FinishItemPreview finish(int segmentSort, String weight) {
        FinishPreviewVO.FinishItemPreview finish = new FinishPreviewVO.FinishItemPreview();
        finish.setSegmentSort(segmentSort);
        finish.setEstimateWeight(weight == null ? null : new BigDecimal(weight));
        return finish;
    }

    private RewindPlanPreviewDTO.RewindSegmentDTO segment(
            int sort, FinishConfigSpecDTO.FinishSourceDTO... sources) {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setSegmentSort(sort);
        segment.setSources(List.of(sources));
        return segment;
    }

    private FinishConfigSpecDTO.FinishSourceDTO source(String uuid, String consumeRatio) {
        FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
        source.setOriginalUuid(uuid);
        source.setShareRatio(new BigDecimal("100.00"));
        source.setConsumeRatio(new BigDecimal(consumeRatio));
        return source;
    }
}
