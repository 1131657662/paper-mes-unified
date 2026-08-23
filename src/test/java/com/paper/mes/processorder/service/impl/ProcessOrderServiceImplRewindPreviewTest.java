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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertEquals(new BigDecimal("11"), preview.getCalculatedLossWeight());
        assertEquals(BigDecimal.ZERO.setScale(0), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("789"), preview.getTotalEstimateWeight());
    }

    @Test
    void buildRewindPreview_equalWidthPiecesUsesIntegerClosedAllocation() {
        OriginalRoll source = roll();
        source.setOriginalWidth(2400);
        source.setRollWeight(new BigDecimal("1862"));
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("LOSS");
        dto.setSegments(List.of(segment(item("FINISH", 800, 3))));

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", source, dto);

        assertEquals(List.of(new BigDecimal("621"), new BigDecimal("621"), new BigDecimal("620")),
                preview.getFinishes().stream()
                        .map(FinishPreviewVO.FinishItemPreview::getEstimateWeight).toList());
        assertEquals(new BigDecimal("1862"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("1862"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()).add(preview.getCalculatedLossWeight()));
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
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight());
        assertEquals(BigDecimal.ZERO.setScale(0), preview.getTotalTrimWeight());
        assertEquals(BigDecimal.ZERO.setScale(0), preview.getCalculatedLossWeight());
    }

    @Test
    void buildRewindPreview_allocateSharesGapAcrossFinishAndExplicitTrim() {
        FinishPreviewVO preview = previewWithPolicy("ALLOCATE", segment(
                item("FINISH", 1400, 1), item("TRIM", 80, 1)));

        assertEquals(20, preview.getWidthDifference());
        assertEquals(new BigDecimal("757"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("43"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()));
    }

    @Test
    void buildRewindPreview_lossPreservesExplicitTrimAndLosesOnlyGap() {
        FinishPreviewVO preview = previewWithPolicy("LOSS", segment(
                item("FINISH", 1400, 1), item("TRIM", 80, 1)));

        assertEquals(new BigDecimal("746"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("43"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("11"), preview.getCalculatedLossWeight());
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()).add(preview.getCalculatedLossWeight()));
    }

    @Test
    void buildRewindPreview_remainderRequiresEachSegmentToCloseWidth() {
        assertThrows(BusinessException.class, () -> previewWithPolicy("REMAINDER", segment(
                item("FINISH", 1400, 1), item("TRIM", 80, 1))));

        FinishPreviewVO preview = previewWithPolicy("REMAINDER", segment(
                item("FINISH", 1400, 1), item("TRIM", 100, 1)));
        assertEquals(0, preview.getWidthDifference());
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight()
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

        assertEquals(new BigDecimal("799"), preview.getTotalEstimateWeight());
        assertEquals(new BigDecimal("1"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight()
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
        assertEquals(new BigDecimal("0"), preview.getTotalTrimWeight());
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight());
    }

    @Test
    void mergedSourceTotalWeightHonorsPartialConsumeRatios() {
        OriginalRollMapper mapper = mock(OriginalRollMapper.class);
        OriginalRoll first = roll();
        first.setUuid("source-a");
        first.setRollWeight(new BigDecimal("1000"));
        OriginalRoll second = roll();
        second.setUuid("source-b");
        second.setRollWeight(new BigDecimal("1000"));
        org.mockito.Mockito.when(mapper.selectList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(first, second));
        ProcessOrderServiceImpl service = serviceWithOriginalRollMapper(mapper);

        FinishConfigSpecDTO spec = spec(800, 1);
        spec.setSources(List.of(source("source-a", "50"), source("source-b", "50")));

        BigDecimal total = ReflectionTestUtils.invokeMethod(service, "calcSourceTotalWeight",
                "order-1", List.of(spec));

        assertEquals(new BigDecimal("1000.00"), total);
    }

    @Test
    void buildRewindPreview_weightSplitIgnoresDifferentDiameterAreas() {
        RewindPlanPreviewDTO dto = diameterSplitPlan("WEIGHT_SPLIT");

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll(), dto);

        assertEquals(List.of(new BigDecimal("400"), new BigDecimal("400")),
                preview.getFinishes().stream()
                        .map(FinishPreviewVO.FinishItemPreview::getEstimateWeight).toList());
    }

    @Test
    void buildRewindPreview_weightSplitDividesSegmentRatioAcrossFinishPieces() {
        RewindPlanPreviewDTO.RewindSegmentDTO first = segment(item("FINISH", 1500, 2));
        first.setSegmentRatio(new BigDecimal("50"));
        first.setTargetDiameter(1000);
        first.setFinishCoreDiameter(3);
        RewindPlanPreviewDTO.RewindSegmentDTO second = segment(item("FINISH", 1500, 1));
        second.setSegmentRatio(new BigDecimal("50"));
        second.setTargetDiameter(1200);
        second.setFinishCoreDiameter(3);
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setAllocationRule("WEIGHT_SPLIT");
        dto.setRewindMode(2);
        dto.setSegments(List.of(first, second));

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll(), dto);

        assertEquals(List.of(new BigDecimal("200"), new BigDecimal("200"),
                        new BigDecimal("400")),
                preview.getFinishes().stream()
                        .map(FinishPreviewVO.FinishItemPreview::getEstimateWeight).toList());
        assertEquals(List.of(new BigDecimal("0.250000"), new BigDecimal("0.250000"),
                        new BigDecimal("0.500000")),
                preview.getFinishes().stream()
                        .map(FinishPreviewVO.FinishItemPreview::getSegmentRatio).toList());
    }

    @Test
    void previewTotalWeight_unknownSourceIgnoresLegacyPlaceholderWeight() {
        OriginalRoll unknown = roll();
        unknown.setUuid("roll-unknown");
        unknown.setWeightStatus("UNKNOWN");
        unknown.setRollWeight(BigDecimal.ONE);
        unknown.setTotalWeight(new BigDecimal("3"));
        unknown.setPieceNum(3);
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(5);
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(item("FINISH", 1500, 1));
        FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
        source.setOriginalUuid(unknown.getUuid());
        segment.setSources(List.of(source));
        dto.setSegments(List.of(segment));

        BigDecimal total = ReflectionTestUtils.invokeMethod(service(), "previewTotalWeight",
                roll(), dto, Map.of(unknown.getUuid(), unknown));

        assertEquals(null, total);
    }

    @Test
    void buildRewindPreview_withoutAllocationRuleKeepsHistoricalAreaAllocation() {
        RewindPlanPreviewDTO dto = diameterSplitPlan(null);

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll(), dto);

        assertTrue(preview.getFinishes().getFirst().getEstimateWeight()
                .compareTo(preview.getFinishes().getLast().getEstimateWeight()) < 0);
        assertEquals(new BigDecimal("800"), preview.getTotalEstimateWeight());
    }

    @Test
    void buildRewindSaveSpecs_weightSplitPersistsEffectivePercentages() {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        RewindPlanPreviewDTO preview = diameterSplitPlan("WEIGHT_SPLIT");
        dto.setAllocationRule(preview.getAllocationRule());
        dto.setRewindMode(preview.getRewindMode());
        dto.setRewindSegments(preview.getSegments());

        List<FinishConfigSpecDTO> specs = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindSaveSpecs", "order-1", roll(), dto);

        assertEquals(List.of(new BigDecimal("50.00"), new BigDecimal("50.00")),
                specs.stream().map(FinishConfigSpecDTO::getSplitRatio).toList());
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
        assertEquals(new BigDecimal("11"), trim.getEstimateWeight());
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
        assertEquals(new BigDecimal("11"), trims.stream()
                .map(FinishConfigSpecDTO::getEstimateWeight).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void buildRewindPreview_multipleSourcePieces_expandsPhysicalOutputs() {
        OriginalRoll roll = roll();
        roll.setPieceNum(8);

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll,
                planFor(segment(item("FINISH", 1400, 1), item("TRIM", 100, 1))));

        assertEquals(8, preview.getFinishCount());
        assertEquals(8, preview.getTrimCount());
        assertEquals(new BigDecimal("747"), preview.getFinishes().getFirst().getEstimateWeight());
        assertEquals(new BigDecimal("6400"), preview.getTotalEstimateWeight()
                .add(preview.getTotalTrimWeight()));
    }

    @Test
    void buildRewindPreview_allocateMultipleSourcePieces_keepsRemainderOnLastPhysicalOutput() {
        OriginalRoll roll = roll();
        roll.setPieceNum(8);
        RewindPlanPreviewDTO dto = planFor(segment(item("FINISH", 1400, 1)));
        dto.setWidthDifferencePolicy("ALLOCATE");

        FinishPreviewVO preview = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindPreview", "order-1", roll, dto);

        assertEquals(8, preview.getFinishCount());
        assertEquals(List.of(
                new BigDecimal("800"), new BigDecimal("800"),
                new BigDecimal("800"), new BigDecimal("800"),
                new BigDecimal("800"), new BigDecimal("800"),
                new BigDecimal("800"), new BigDecimal("800")),
                preview.getFinishes().stream()
                        .map(FinishPreviewVO.FinishItemPreview::getEstimateWeight).toList());
        assertEquals(new BigDecimal("6400"), preview.getTotalEstimateWeight());
    }

    @Test
    void buildRewindSaveSpecs_multipleSourcePieces_savesEveryPhysicalOutput() {
        OriginalRoll roll = roll();
        roll.setPieceNum(8);
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("REMAINDER");
        dto.setRewindSegments(List.of(segment(item("FINISH", 1400, 1), item("TRIM", 100, 1))));

        List<FinishConfigSpecDTO> specs = ReflectionTestUtils.invokeMethod(
                service(), "buildRewindSaveSpecs", "order-1", roll, dto);

        assertEquals(16, specs.size());
        assertEquals(8, specs.stream().filter(spec -> "FINISH".equals(spec.getItemType())).count());
        assertEquals(8, specs.stream().filter(spec -> "TRIM".equals(spec.getItemType())).count());
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

    @Test
    void validateSameSpecRewind_usesMeasuredWidthWhenItDiffersFromOriginalWidth() {
        OriginalRoll roll = roll();
        roll.setActualWidth(1490);

        RewindPlanPreviewDTO dto = sameSpecPlan(1490, 48, 6);

        ReflectionTestUtils.invokeMethod(service(), "validateSameSpecRewind", dto, roll);
    }

    @Test
    void validateSameSpecRewind_rejectsMissingSourceDiameter() {
        OriginalRoll roll = roll();
        roll.setOriginalDiameter(null);

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service(), "validateSameSpecRewind", sameSpecPlan(1500, 48, 6), roll));
    }

    @Test
    void validateSameSpecRewind_rejectsMissingSourceCore() {
        OriginalRoll roll = roll();
        roll.setCoreDiameter(null);

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service(), "validateSameSpecRewind", sameSpecPlan(1500, 48, 6), roll));
    }

    @Test
    void validateRewindSegmentSources_rejectsCurrentPlanOwnerAsSource() {
        OriginalRoll owner = roll();
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(item("FINISH", 750, 2));
        segment.setSources(List.of(source(owner.getUuid(), "100")));

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service(), "validateRewindSegmentSources", List.of(segment),
                Map.of(owner.getUuid(), owner), owner));
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

    private RewindPlanPreviewDTO diameterSplitPlan(String allocationRule) {
        RewindPlanPreviewDTO.RewindSegmentDTO first = segment(item("FINISH", 1500, 1));
        first.setSegmentRatio(new BigDecimal("50"));
        first.setTargetDiameter(1000);
        first.setFinishCoreDiameter(3);
        RewindPlanPreviewDTO.RewindSegmentDTO second = segment(item("FINISH", 1500, 1));
        second.setSegmentRatio(new BigDecimal("50"));
        second.setTargetDiameter(1200);
        second.setFinishCoreDiameter(3);
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setAllocationRule(allocationRule);
        dto.setRewindMode(2);
        dto.setSegments(List.of(first, second));
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

    private RewindPlanPreviewDTO planFor(RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(1);
        dto.setWidthDifferencePolicy("REMAINDER");
        dto.setSegments(List.of(segment));
        return dto;
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
        return serviceWithOriginalRollMapper(mock(OriginalRollMapper.class));
    }

    private ProcessOrderServiceImpl serviceWithOriginalRollMapper(OriginalRollMapper originalRollMapper) {
        return new ProcessOrderServiceImpl(
                originalRollMapper,
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
                mock(com.paper.mes.processorder.service.ProcessStepRouteMutationGuard.class),
                null,
                null,
                null,
                new com.paper.mes.processorder.service.ProcessOrderSettlementPolicy(),
                mock(InventoryLedgerBusinessRecorder.class));
    }

    private FinishConfigSpecDTO.FinishSourceDTO source(String uuid, String ratio) {
        FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
        source.setOriginalUuid(uuid);
        source.setConsumeRatio(new BigDecimal(ratio));
        return source;
    }
}
