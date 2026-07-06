package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.FileStorageService;
import com.paper.mes.processorder.service.ProcessOrderExportService;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProcessOrderServiceImplRewindPreviewTest {

    @Test
    void buildRewindPreview_mode1_infersRemainingWidthAsTrim() {
        FinishPreviewVO preview = preview(1, segment(
                item("FINISH", 500, 2),
                item("FINISH", 480, 1)
        ));

        assertEquals(3, preview.getFinishCount());
        assertEquals(1, preview.getTrimCount());
        assertEquals(20, preview.getSegments().getFirst().getTrimWidth());
        assertEquals(new BigDecimal("10.667"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("789.333"), preview.getTotalEstimateWeight());
    }

    @Test
    void buildRewindPreview_mode2_neverInfersHorizontalTrim() {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(
                item("FINISH", 500, 2)
        );
        segment.setTargetDiameter(30);
        segment.setFinishCoreDiameter(3);

        FinishPreviewVO preview = preview(2, segment);

        assertEquals(2, preview.getFinishCount());
        assertEquals(0, preview.getTrimCount());
        assertEquals(new BigDecimal("0.000"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("800.000"), preview.getTotalEstimateWeight());
    }

    @Test
    void buildRewindPreview_mode2_rejectsExplicitTrim() {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(
                item("FINISH", 1500, 1),
                item("TRIM", 20, 1)
        );
        segment.setTargetDiameter(30);
        segment.setFinishCoreDiameter(3);

        assertThrows(BusinessException.class, () -> preview(2, segment));
    }

    @Test
    void buildRewindSaveSpecs_usesSegmentTrimWeightForSavedTrimRow() {
        ProcessOrderServiceImpl service = service();
        OriginalRoll roll = roll();
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(1);
        dto.setRewindSegments(List.of(segment(
                item("FINISH", 500, 2),
                item("FINISH", 480, 1)
        )));

        List<FinishConfigSpecDTO> specs = ReflectionTestUtils.invokeMethod(
                service, "buildRewindSaveSpecs", "order-1", roll, dto);

        assertEquals(4, specs.size());
        FinishConfigSpecDTO trim = specs.get(3);
        assertEquals("TRIM", trim.getItemType());
        assertEquals(20, trim.getFinishWidth());
        assertEquals(new BigDecimal("10.667"), trim.getEstimateWeight());
    }

    @Test
    void buildRewindSaveSpecs_whenOnSite_reservesNumbersWithoutTrimOrWeights() {
        ProcessOrderServiceImpl service = service();
        OriginalRoll roll = roll();
        roll.setProcessMode(2);
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(2);
        dto.setFinishSpecs(List.of(spec(0, 2)));

        List<FinishConfigSpecDTO> specs = ReflectionTestUtils.invokeMethod(
                service, "buildRewindSaveSpecs", "order-1", roll, dto);

        assertEquals(2, specs.size());
        assertEquals("FINISH", specs.getFirst().getItemType());
        assertEquals(new BigDecimal("0.000"), specs.getFirst().getEstimateWeight());
    }

    private FinishPreviewVO preview(int rewindMode, RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(rewindMode);
        dto.setSegments(List.of(segment));
        return ReflectionTestUtils.invokeMethod(service(), "buildRewindPreview", "order-1", roll(), dto);
    }

    private RewindPlanPreviewDTO.RewindSegmentDTO segment(RewindPlanPreviewDTO.RewindLayoutItemDTO... items) {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = new RewindPlanPreviewDTO.RewindSegmentDTO();
        segment.setSegmentSort(1);
        segment.setSegmentRatio(BigDecimal.ONE);
        segment.setRepeatCount(1);
        segment.setLayoutItems(List.of(items));
        return segment;
    }

    private RewindPlanPreviewDTO.RewindLayoutItemDTO item(String itemType, int width, int quantity) {
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = new RewindPlanPreviewDTO.RewindLayoutItemDTO();
        item.setItemType(itemType);
        item.setWidth(width);
        item.setQuantity(quantity);
        return item;
    }

    private FinishConfigSpecDTO spec(int width, int count) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType("FINISH");
        spec.setFinishWidth(width);
        spec.setCount(count);
        return spec;
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setProcessMode(1);
        roll.setOriginalWidth(1500);
        roll.setRollWeight(new BigDecimal("800.000"));
        roll.setPieceNum(1);
        return roll;
    }

    private ProcessOrderServiceImpl service() {
        return new ProcessOrderServiceImpl(
                mock(OriginalRollMapper.class),
                mock(FinishRollMapper.class),
                mock(ProcessStepMapper.class),
                mock(ProcessParamMapper.class),
                mock(ProcessStageInputRelMapper.class),
                mock(ProcessStageOutputMapper.class),
                mock(FinishOriginalRelMapper.class),
                mock(DeliveryDetailMapper.class),
                mock(SettleDetailMapper.class),
                mock(CustomerService.class),
                mock(OperationLogService.class),
                new ObjectMapper(),
                mock(FileStorageService.class),
                mock(RollNoSequenceService.class),
                new SawPlanPreviewer(),
                mock(DocumentNoService.class),
                mock(ProcessOrderExportService.class),
                mock(BusinessLockService.class),
                mock(MachineMapper.class),
                mock(WeightCheckThresholdService.class));
    }
}
