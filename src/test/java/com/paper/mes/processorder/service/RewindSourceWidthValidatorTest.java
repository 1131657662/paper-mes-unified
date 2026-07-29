package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewindSourceWidthValidatorTest {

    @Test
    void requireSameWidth_acceptsMatchingEffectiveWidths() {
        OriginalRoll owner = roll("owner", 1500, null);
        OriginalRoll source = roll("source", 1490, 1500);

        assertDoesNotThrow(() -> RewindSourceWidthValidator.requireSameWidth(
                owner, List.of(segment("source")), Map.of("source", source)));
    }

    @Test
    void requireSameWidth_rejectsDifferentSourceWidth() {
        OriginalRoll owner = roll("owner", 1500, null);
        OriginalRoll source = roll("source", 1400, null);

        assertThrows(BusinessException.class, () -> RewindSourceWidthValidator.requireSameWidth(
                owner, List.of(segment("source")), Map.of("source", source)));
    }

    private RewindPlanPreviewDTO.RewindSegmentDTO segment(String sourceUuid) {
        FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
        source.setOriginalUuid(sourceUuid);
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setSources(List.of(source));
        return segment;
    }

    private OriginalRoll roll(String uuid, int originalWidth, Integer actualWidth) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setOriginalWidth(originalWidth);
        roll.setActualWidth(actualWidth);
        return roll;
    }
}
