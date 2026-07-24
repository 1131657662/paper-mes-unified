package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinishConfigQuantityValidatorTest {

    @Test
    void requireWithinLimit_whenExpandedTotalIs500_accepts() {
        FinishConfigSaveDTO config = config(spec(490), 10);

        assertDoesNotThrow(() -> FinishConfigQuantityValidator.requireWithinLimit(config));
    }

    @Test
    void requireWithinLimit_whenExpandedTotalExceeds500_rejects() {
        FinishConfigSaveDTO config = config(spec(500), 1);

        assertThrows(BusinessException.class,
                () -> FinishConfigQuantityValidator.requireWithinLimit(config));
    }

    @Test
    void requireWithinLimit_whenPreviewExpandsTo500_accepts() {
        RewindPlanPreviewDTO preview = preview(50, 10, 0);

        assertDoesNotThrow(() -> FinishConfigQuantityValidator.requireWithinLimit(preview));
    }

    @Test
    void requireWithinLimit_whenPreviewExpandsBeyond500_rejects() {
        RewindPlanPreviewDTO preview = preview(51, 10, 0);

        assertThrows(BusinessException.class,
                () -> FinishConfigQuantityValidator.requireWithinLimit(preview));
    }

    @Test
    void requireWithinLimit_whenPreviewUsesExtremeRepeatCount_rejectsWithoutExpanding() {
        RewindPlanPreviewDTO preview = preview(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);

        assertThrows(BusinessException.class,
                () -> FinishConfigQuantityValidator.requireWithinLimit(preview));
    }

    @Test
    void requireWithinLimit_whenPreviewQuantityIsNull_defaultsToOne() {
        RewindPlanPreviewDTO preview = preview(500, 1, 0);
        preview.getSegments().getFirst().getLayoutItems().getFirst().setQuantity(null);

        assertDoesNotThrow(() -> FinishConfigQuantityValidator.requireWithinLimit(preview));
    }

    @Test
    void requireWithinLimit_whenDraftPlanUsesExtremeCounts_rejectsWithoutExpanding() {
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setQuantity(Integer.MAX_VALUE);
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setRepeatCount(Integer.MAX_VALUE);
        segment.setLayoutItems(List.of(item));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setSegments(List.of(segment));

        assertThrows(BusinessException.class, () -> FinishConfigQuantityValidator.requireWithinLimit(plan));
    }

    private FinishConfigSaveDTO config(FinishConfigSpecDTO spec, int spareCount) {
        FinishConfigSaveDTO config = new FinishConfigSaveDTO();
        config.setFinishSpecs(List.of(spec));
        config.setSpareCount(spareCount);
        return config;
    }

    private FinishConfigSpecDTO spec(int count) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setCount(count);
        return spec;
    }

    private RewindPlanPreviewDTO preview(int repeatCount, int quantity, int spareCount) {
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = new RewindPlanPreviewDTO.RewindLayoutItemDTO();
        item.setQuantity(quantity);
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setRepeatCount(repeatCount);
        segment.setLayoutItems(List.of(item));
        RewindPlanPreviewDTO preview = new RewindPlanPreviewDTO();
        preview.setSegments(List.of(segment));
        preview.setSpareCount(spareCount);
        return preview;
    }
}
