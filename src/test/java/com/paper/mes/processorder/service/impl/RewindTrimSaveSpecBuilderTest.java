package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RewindTrimSaveSpecBuilderTest {

    @Test
    void build_repeatedSegmentCreatesOneTrimRollPerRepeatAndClosesWeight() {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(2,
                item("FINISH", 1480, 1), item("TRIM", 20, 1));
        FinishConfigSaveDTO dto = dto("REMAINDER", segment);
        FinishPreviewVO preview = preview("789.333", "10.667", "0.000");

        List<FinishConfigSpecDTO> specs = RewindTrimSaveSpecBuilder.build(preview, dto);

        assertThat(specs).extracting(FinishConfigSpecDTO::getFinishWidth).containsExactly(20, 20);
        assertThat(specs).extracting(FinishConfigSpecDTO::getEstimateWeight)
                .containsExactly(new BigDecimal("5.333"), new BigDecimal("5.334"));
    }

    @Test
    void build_allocatePreservesDistinctTrimPiecesAndAllocationShare() {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(1,
                item("FINISH", 1400, 1), item("TRIM", 40, 2));
        FinishConfigSaveDTO dto = dto("ALLOCATE", segment);
        FinishPreviewVO preview = preview("750.221", "49.779", "0.000");

        List<FinishConfigSpecDTO> specs = RewindTrimSaveSpecBuilder.build(preview, dto);

        assertThat(specs).extracting(FinishConfigSpecDTO::getFinishWidth).containsExactly(40, 40);
        assertThat(specs).extracting(FinishConfigSpecDTO::getEstimateWeight)
                .containsExactly(new BigDecimal("24.889"), new BigDecimal("24.890"));
    }

    @Test
    void build_manyNarrowTrimRowsNeverCreatesNegativeRoundingTail() {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(1,
                item("FINISH", 1000, 1), item("TRIM", 1, 500));
        FinishConfigSaveDTO dto = dto("REMAINDER", segment);
        FinishPreviewVO preview = preview("0.667", "0.333", "0.000");

        List<FinishConfigSpecDTO> specs = RewindTrimSaveSpecBuilder.build(preview, dto);

        assertThat(specs).hasSize(500);
        assertThat(specs).allMatch(spec -> spec.getEstimateWeight().signum() >= 0);
        assertThat(specs).extracting(FinishConfigSpecDTO::getEstimateWeight)
                .containsOnly(new BigDecimal("0.000"), new BigDecimal("0.001"));
        BigDecimal total = specs.stream().map(FinishConfigSpecDTO::getEstimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(new BigDecimal("0.333"));
    }

    private FinishConfigSaveDTO dto(String policy, RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy(policy);
        dto.setRewindSegments(List.of(segment));
        return dto;
    }

    private FinishPreviewVO preview(String finishWeight, String trimWeight, String lossWeight) {
        FinishPreviewVO.SegmentPreview segment = new FinishPreviewVO.SegmentPreview();
        segment.setSegmentRatio(BigDecimal.ONE);
        FinishPreviewVO preview = new FinishPreviewVO();
        preview.setOriginalWidth(1500);
        preview.setTotalEstimateWeight(new BigDecimal(finishWeight));
        preview.setTotalTrimWeight(new BigDecimal(trimWeight));
        preview.setCalculatedLossWeight(new BigDecimal(lossWeight));
        preview.setSegments(List.of(segment));
        return preview;
    }

    private RewindPlanPreviewDTO.RewindSegmentDTO segment(
            int repeatCount, RewindPlanPreviewDTO.RewindLayoutItemDTO... items) {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setRepeatCount(repeatCount);
        segment.setLayoutItems(List.of(items));
        return segment;
    }

    private RewindPlanPreviewDTO.RewindLayoutItemDTO item(String type, int width, int quantity) {
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = new RewindPlanPreviewDTO.RewindLayoutItemDTO();
        item.setItemType(type);
        item.setWidth(width);
        item.setQuantity(quantity);
        return item;
    }
}
