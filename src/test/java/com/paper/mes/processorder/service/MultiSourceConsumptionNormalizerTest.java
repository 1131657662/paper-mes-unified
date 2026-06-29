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
}
