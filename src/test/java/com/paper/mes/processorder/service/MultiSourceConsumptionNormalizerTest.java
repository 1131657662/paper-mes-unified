package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiSourceConsumptionNormalizerTest {

    @Test
    void normalize_withConsumptionRatios_convertsSegmentCompositionByWeight() {
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(
                segment(source("roll-3", "70")),
                segment(source("roll-3", "30"), source("roll-4", "30")),
                segment(source("roll-4", "70"))
        );
        Map<String, OriginalRoll> rolls = Map.of(
                "roll-3", roll("roll-3", "1000"),
                "roll-4", roll("roll-4", "2000")
        );

        MultiSourceConsumptionNormalizer.normalize(segments, rolls);

        assertEquals(new BigDecimal("100.00"), segments.get(0).getSources().getFirst().getShareRatio());
        assertEquals(new BigDecimal("33.33"), segments.get(1).getSources().get(0).getShareRatio());
        assertEquals(new BigDecimal("66.67"), segments.get(1).getSources().get(1).getShareRatio());
        assertEquals(new BigDecimal("100.00"), segments.get(2).getSources().getFirst().getShareRatio());
        assertEquals(new BigDecimal("3000.000"), MultiSourceConsumptionNormalizer.totalConsumedWeight(segments, rolls));
    }

    @Test
    void normalize_whenSourceConsumptionIsIncomplete_throws() {
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(segment(source("roll-3", "70")));
        Map<String, OriginalRoll> rolls = Map.of("roll-3", roll("roll-3", "1000"));

        assertThrows(BusinessException.class, () -> MultiSourceConsumptionNormalizer.normalize(segments, rolls));
    }

    @Test
    void normalize_whenSourceConsumptionTotalsExceedOneHundredPercent_throws() {
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(
                segment(source("roll-3", "70")),
                segment(source("roll-3", "40")));
        Map<String, OriginalRoll> rolls = Map.of("roll-3", roll("roll-3", "1000"));

        assertThrows(BusinessException.class, () -> MultiSourceConsumptionNormalizer.normalize(segments, rolls));
    }

    @Test
    void totalConsumedWeight_prefersMeasuredWeightOverEstimatedWeight() {
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(segment(source("roll-1", "100")));
        OriginalRoll roll = roll("roll-1", "1");
        roll.setActualWeight(new BigDecimal("2000"));
        roll.setWeightStatus("MEASURED");

        assertEquals(new BigDecimal("2000.000"),
                MultiSourceConsumptionNormalizer.totalConsumedWeight(segments, Map.of("roll-1", roll)));
    }

    @Test
    void totalConsumedWeight_returnsNullWhenSourceWeightIsUnknown() {
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(segment(source("roll-1", "100")));
        OriginalRoll roll = roll("roll-1", "1");
        roll.setRollWeight(null);
        roll.setWeightStatus("UNKNOWN");

        assertEquals(null,
                MultiSourceConsumptionNormalizer.totalConsumedWeight(segments, Map.of("roll-1", roll)));
    }

    @Test
    void normalize_whenSourceWeightsAreUnknown_keepsCompositionPending() {
        FinishConfigSpecDTO.FinishSourceDTO first = source("roll-1", "100");
        FinishConfigSpecDTO.FinishSourceDTO second = source("roll-2", "100");
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(segment(first, second));
        Map<String, OriginalRoll> rolls = Map.of(
                "roll-1", unknownRoll("roll-1"),
                "roll-2", unknownRoll("roll-2"));

        MultiSourceConsumptionNormalizer.normalize(segments, rolls);

        assertNull(first.getShareRatio());
        assertNull(second.getShareRatio());
        assertNull(MultiSourceConsumptionNormalizer.totalConsumedWeight(segments, rolls));
    }

    @Test
    void normalize_whenUnknownWeightsHavePartialExplicitShares_throwsBusinessError() {
        FinishConfigSpecDTO.FinishSourceDTO first = source("roll-1", "100");
        first.setShareRatio(new BigDecimal("50"));
        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments = List.of(
                segment(first, source("roll-2", "100")));
        Map<String, OriginalRoll> rolls = Map.of(
                "roll-1", unknownRoll("roll-1"),
                "roll-2", unknownRoll("roll-2"));

        assertThrows(BusinessException.class,
                () -> MultiSourceConsumptionNormalizer.normalize(segments, rolls));
    }

    private RewindPlanPreviewDTO.RewindSegmentDTO segment(FinishConfigSpecDTO.FinishSourceDTO... sources) {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setSources(List.of(sources));
        return segment;
    }

    private FinishConfigSpecDTO.FinishSourceDTO source(String uuid, String consumeRatio) {
        FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
        source.setOriginalUuid(uuid);
        source.setConsumeRatio(new BigDecimal(consumeRatio));
        return source;
    }

    private OriginalRoll roll(String uuid, String weight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRollWeight(new BigDecimal(weight));
        roll.setPieceNum(1);
        return roll;
    }

    private OriginalRoll unknownRoll(String uuid) {
        OriginalRoll roll = roll(uuid, "1");
        roll.setRollWeight(null);
        roll.setWeightStatus("UNKNOWN");
        return roll;
    }
}
