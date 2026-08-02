package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
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
import com.paper.mes.processorder.service.DamageImageService;
import com.paper.mes.processorder.service.BackRecordWarehousePolicy;
import com.paper.mes.processorder.service.BackRecordScopeResolver;
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
    void buildRewindPreview_lossLeavesUnassignedWidthOutOfInventory() {
        FinishPreviewVO preview = preview(1, segment(
                item("FINISH", 500, 2),
                item("FINISH", 480, 1)
        ));

        assertEquals(3, preview.getFinishCount());
        assertEquals(0, preview.getTrimCount());
        assertEquals(20, preview.getWidthDifference());
        assertEquals(new BigDecimal("10.667"), preview.getCalculatedLossWeight());
        assertEquals(BigDecimal.ZERO.setScale(3), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("789.333"), preview.getTotalEstimateWeight());
    }

    @Test
    void buildRewindPreview_allocateClosesWeightWithoutCreatingImplicitTrim() {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("ALLOCATE");
        dto.setSegments(List.of(segment(
                item("FINISH", 500, 2), item("FINISH", 480, 1))));

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll(), dto);

        assertEquals(20, preview.getWidthDifference());
        assertEquals(new BigDecimal("800.000"), preview.getTotalEstimateWeight());
        assertEquals(BigDecimal.ZERO.setScale(3), preview.getTotalTrimWeight());
        assertEquals(BigDecimal.ZERO.setScale(3), preview.getCalculatedLossWeight());
    }

    @Test
    void buildRewindPreview_allocateSharesGapAcrossFinishAndExplicitTrim() {
        FinishPreviewVO preview = previewWithPolicy("ALLOCATE", segment(
                item("FINISH", 1400, 1), item("TRIM", 80, 1)));

        assertEquals(20, preview.getWidthDifference());
        assertEquals(new BigDecimal("751.999"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("48.001"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("800.000"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()));
    }

    @Test
    void buildRewindPreview_lossPreservesExplicitTrimAndLosesOnlyGap() {
        FinishPreviewVO preview = previewWithPolicy("LOSS", segment(
                item("FINISH", 1400, 1), item("TRIM", 80, 1)));

        assertEquals(new BigDecimal("746.666"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("42.667"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("10.667"), preview.getCalculatedLossWeight());
        assertEquals(new BigDecimal("800.000"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()).add(preview.getCalculatedLossWeight()));
    }

    @Test
    void buildRewindPreview_remainderRequiresEachSegmentToCloseWidth() {
        assertThrows(BusinessException.class, () -> previewWithPolicy("REMAINDER", segment(
                item("FINISH", 1400, 1), item("TRIM", 80, 1))));

        FinishPreviewVO preview = previewWithPolicy("REMAINDER", segment(
                item("FINISH", 1400, 1), item("TRIM", 100, 1)));
        assertEquals(0, preview.getWidthDifference());
        assertEquals(new BigDecimal("800.000"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()));
    }

    @Test
    void buildRewindPreview_multipleSegmentsClosesPerSegmentTrimRounding() {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("REMAINDER");
        dto.setSegments(List.of(
                segment(item("FINISH", 1499, 1), item("TRIM", 1, 1)),
                segment(item("FINISH", 1499, 1), item("TRIM", 1, 1))));

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll(), dto);

        assertEquals(new BigDecimal("799.466"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("0.534"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("800.000"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()).add(preview.getCalculatedLossWeight()));
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
    void buildRewindPreview_mode4_uses_layer_spec_instead_of_stale_segment_values() {
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = item("FINISH", 1500, 1);
        FinishConfigSpecDTO.FinishLayerDTO layer = new FinishConfigSpecDTO.FinishLayerDTO();
        layer.setOutDiameter(900);
        layer.setCoreDiameter(6);
        item.setLayers(List.of(layer));
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(item);
        segment.setTargetDiameter(1000);
        segment.setFinishCoreDiameter(3);

        FinishPreviewVO preview = preview(4, segment);

        assertEquals(900, preview.getFinishes().getFirst().getFinishDiameter());
        assertEquals(6, preview.getFinishes().getFirst().getFinishCoreDiameter());
    }

    @Test
    void validateCustomerSpecifications_rejects_override_without_reason() {
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = item("FINISH", 1500, 1);
        item.setCustomerGramWeight(90);
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setSegments(List.of(segment(item)));
        OriginalRoll roll = roll();
        roll.setPaperName("白卡纸");
        roll.setGramWeight(80);

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service(), "validateCustomerSpecifications", dto, roll));
    }

    @Test
    void buildRewindSaveSpecs_usesSegmentTrimWeightForSavedTrimRow() {
        ProcessOrderServiceImpl service = service();
        OriginalRoll roll = roll();
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("REMAINDER");
        dto.setRewindSegments(List.of(segment(
                item("FINISH", 500, 2),
                item("FINISH", 480, 1),
                item("TRIM", 20, 1)
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
    void buildRewindSaveSpecs_repeatedSegmentPreservesTrimRollCount() {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("REMAINDER");
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(
                item("FINISH", 1480, 1), item("TRIM", 20, 1));
        segment.setRepeatCount(2);
        dto.setRewindSegments(List.of(segment));

        List<FinishConfigSpecDTO> specs = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindSaveSpecs", "order-1", roll(), dto);

        List<FinishConfigSpecDTO> trims = specs.stream()
                .filter(spec -> "TRIM".equals(spec.getItemType())).toList();
        assertEquals(2, trims.size());
        assertEquals(new BigDecimal("10.667"), trims.stream()
                .map(FinishConfigSpecDTO::getEstimateWeight).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void validateRewindPreviewPlan_normalizesDuplicateSegmentSorts() {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setSegments(List.of(segment(item("FINISH", 1500, 1)),
                segment(item("FINISH", 1500, 1))));

        ReflectionTestUtils.invokeMethod(service(), "validateRewindPreviewPlan", dto);

        assertEquals(List.of(1, 2), dto.getSegments().stream()
                .map(RewindPlanPreviewDTO.RewindSegmentDTO::getSegmentSort).toList());
    }

    @Test
    void validateRewindPreviewPlan_whenSegmentContainsOnlyTrim_rejectsPlan() {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setSegments(List.of(segment(item("TRIM", 1500, 1))));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(service(), "validateRewindPreviewPlan", dto));

        assertEquals("每个直径分段至少需要一个正式成品", exception.getMessage());
    }

    @Test
    void buildRewindSaveSpecs_whenOnSite_defersAllOutputsUntilBackRecord() {
        ProcessOrderServiceImpl service = service();
        OriginalRoll roll = roll();
        roll.setProcessMode(2);
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(2);
        dto.setFinishSpecs(List.of(spec(0, 2)));

        List<FinishConfigSpecDTO> specs = ReflectionTestUtils.invokeMethod(
                service, "buildRewindSaveSpecs", "order-1", roll, dto);

        assertEquals(List.of(), specs);
    }

    @Test
    void validateSameSpecRewind_whenSpecificationsMatch_acceptsOneToOnePlan() {
        RewindPlanPreviewDTO dto = sameSpecPlan(1500, 48, 6);

        ReflectionTestUtils.invokeMethod(service(), "validateSameSpecRewind", dto, roll());
    }

    @Test
    void validateSameSpecRewind_whenWidthChanges_rejectsPlan() {
        RewindPlanPreviewDTO dto = sameSpecPlan(1499, 48, 6);

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service(), "validateSameSpecRewind", dto, roll()));
    }

    private RewindPlanPreviewDTO sameSpecPlan(int width, int diameter, int coreDiameter) {
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(item("FINISH", width, 1));
        segment.setTargetDiameter(diameter);
        segment.setFinishCoreDiameter(coreDiameter);
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(6);
        dto.setSegments(List.of(segment));
        return dto;
    }

    private FinishPreviewVO preview(int rewindMode, RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(rewindMode);
        dto.setWidthDifferencePolicy("LOSS");
        dto.setSegments(List.of(segment));
        return ReflectionTestUtils.invokeMethod(service(), "buildRewindPreview", "order-1", roll(), dto);
    }

    private FinishPreviewVO previewWithPolicy(String policy,
                                               RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy(policy);
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
        roll.setOriginalDiameter(48);
        roll.setCoreDiameter(6);
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
                mock(DamageImageService.class),
                mock(RollNoSequenceService.class),
                new SawPlanPreviewer(),
                mock(DocumentNoService.class),
                mock(BusinessLockService.class),
                mock(MachineMapper.class),
                mock(WeightCheckThresholdService.class),
                null,
                null,
                null,
                null,
                new BackRecordScopeResolver(),
                null,
                mock(BackRecordWarehousePolicy.class),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new com.paper.mes.processorder.service.ProcessOrderSettlementPolicy(),
                mock(InventoryLedgerBusinessRecorder.class));
    }
}
