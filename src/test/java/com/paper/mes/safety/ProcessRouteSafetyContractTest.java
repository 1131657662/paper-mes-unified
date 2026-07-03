package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRouteSafetyContractTest {

    private static final String STEP_WRITER =
            "src/main/java/com/paper/mes/processorder/service/ProcessRouteStepWriter.java";
    private static final String CLEANUP_SERVICE =
            "src/main/java/com/paper/mes/processorder/service/ProcessRouteCleanupService.java";
    private static final String SAVE_SERVICE =
            "src/main/java/com/paper/mes/processorder/service/ProcessRouteSaveService.java";
    private static final String APPEND_SERVICE =
            "src/main/java/com/paper/mes/processorder/service/ProcessRouteAppendService.java";
    private static final String INTEGRITY_BOOTSTRAP =
            "src/main/java/com/paper/mes/system/config/config/ProcessRouteIntegrityBootstrap.java";

    @Test
    void processRouteWriter_whenStageConsumesManyOutputs_persistsEachInputRelation() throws IOException {
        String source = source(STEP_WRITER);
        String write = slice(source, "public Map<String, ProcessStageOutput> write", "private ProcessStep buildStep");
        String inputs = slice(source, "private void appendStageInputs", "private void appendStageOutputs");

        assertContainsAll(write,
                "processStepMapper.insert(step);",
                "appendStageInputs(context, stage, step, outputsByKey);",
                "appendStageOutputs(context, stage, preview, step, outputsByKey);");
        assertContainsAll(inputs,
                "for (String inputKey : stage.getInputOutputKeys())",
                "rel.setInputOutputUuid(input.getUuid())",
                "rel.setSourceStepUuid(input.getStepUuid())",
                "rel.setInputSort(sort++)",
                "stageInputRelMapper.insert(rel)");
    }

    @Test
    void processRouteWriter_keepsLegacyFirstInputFieldsForCompatibility() throws IOException {
        String source = source(STEP_WRITER);
        String step = slice(source, "private ProcessStep buildStep", "private void appendStageInputs");

        assertContainsAll(step,
                "ProcessStageOutput parent = firstInputOutput(stage, outputsByKey);",
                "step.setInputOutputUuid(parent == null ? null : parent.getUuid())",
                "step.setParentStepUuid(parent == null ? null : parent.getStepUuid())");
    }

    @Test
    void processRouteCleanup_whenReplacingRoute_removesOldInputRelations() throws IOException {
        String source = source(CLEANUP_SERVICE);
        String cleanup = slice(source, "public void clearExistingRoute", "private void voidExistingFinishes");

        assertContainsAll(cleanup,
                "stageInputRelMapper.delete",
                "ProcessStageInputRel::getOriginalUuid",
                "roll.getUuid()");
    }

    @Test
    void processRouteSave_whenReplacingRoute_allowsOnlyPendingOrders() throws IOException {
        String source = source(SAVE_SERVICE);
        String save = slice(source, "public ProcessRoutePreviewVO save", "private ProcessRouteContext loadContext");
        String loadContext = slice(source, "private ProcessRouteContext loadContext", "private void requireFinalOutputs");

        assertContainsAll(save,
                "ProcessRouteContext context = loadContext(orderUuid, dto.getOriginalUuid());",
                "persistenceService.replaceRoute(context, dto, preview);");
        assertContainsAll(loadContext,
                "order.getOrderStatus() == null || order.getOrderStatus() != STATUS_PENDING",
                "工艺路线配置会重建工序和成品号，仅待下发加工单可操作；待回录请使用追加后续链式工艺");
    }

    @Test
    void processRouteAppend_whenSavingRoute_neverClearsExistingRoute() throws IOException {
        String source = source(APPEND_SERVICE);
        String save = slice(source, "public ProcessRoutePreviewVO save", "private ProcessRouteContext loadContext");

        assertContainsAll(save,
                "outputResolver.resolveForSave(context, dto)",
                "stepWriter.writeAppend(",
                "sourceConsumer.consume(sources.values())",
                "finishWriter.createFinalFinishes(context, preview, outputsByKey)",
                "processOrderService.calcFee(orderUuid)");
        assertTrue(!save.contains("clearExistingRoute"),
                "追加后续工艺不能清理已有工序、阶段产物或成品号");
    }

    @Test
    void processRouteAppend_allowsOnlyPendingOrToRecordOrders() throws IOException {
        String source = source(APPEND_SERVICE);
        String canAppend = slice(source, "private boolean canAppend", "private void requireAppendStages");

        assertContainsAll(source,
                "private static final int STATUS_PENDING = 1",
                "private static final int STATUS_TO_RECORD = 3",
                "仅待下发或待回录加工单可追加后续链式工艺");
        assertContainsAll(canAppend,
                "status == STATUS_PENDING || status == STATUS_TO_RECORD");
    }

    @Test
    void processRouteBootstrap_whenDatabaseStarts_createsStageInputRelationTable() throws IOException {
        String source = source(INTEGRITY_BOOTSTRAP);

        assertContainsAll(source,
                "createStageInputRelTable();",
                "CREATE TABLE IF NOT EXISTS `biz_process_stage_input_rel`",
                "`input_output_uuid` VARCHAR(36)   NOT NULL",
                "KEY `idx_input_output_uuid` (`input_output_uuid`)");
    }

    @Test
    void processOrderFeeAndStepChanges_useSingleMixProcessRule() throws IOException {
        String source = source("src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java");
        String calcFee = slice(source, "public FeeResultVO calcFee", "private BigDecimal sumExtraFees");
        String updateFlag = slice(source, "private void updateMixProcessFlag", "}");

        assertContainsAll(calcFee,
                "order.setIsMixProcess(ProcessMixProcessResolver.isMix(steps) ? 1 : 0)");
        assertContainsAll(updateFlag,
                "orderToUpdate.setIsMixProcess(ProcessMixProcessResolver.isMix(steps) ? 1 : 0)");
        assertTrue(!calcFee.contains("hasSaw && hasRewind"), "calcFee must not override mix flag with the old narrow rule");
    }

    @Test
    void processOrderDetail_whenMappingStageOutputs_exposesRouteParentFields() throws IOException {
        String source = source("src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java");
        String mapper = slice(source, "private ProcessOrderDetailVO.StageOutputVO toDetailStageOutput", "private List<ProcessOrderDetailVO.RewindParamVO>");

        assertContainsAll(mapper,
                "item.setParentOutputUuid(output.getParentOutputUuid())",
                "item.setSourceStepType(output.getSourceStepType())",
                "item.setSourceSummary(output.getSourceSummary())");
    }

    @Test
    void processOrderRewindPreviewAndSave_useSharedWeightCalculator() throws IOException {
        String source = source("src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java");
        String preview = slice(source,
                "private List<FinishPreviewVO.FinishItemPreview> allocatePreviewWeights",
                "private Map<String, OriginalRoll> orderRollMap");
        String save = slice(source,
                "private List<FinishConfigSpecDTO> buildRewindSaveSpecs",
                "private List<FinishConfigSpecDTO.FinishSourceDTO> resolveSegmentSources");

        assertContainsAll(preview,
                "RewindWeightCalculator.allocate(",
                "result.weight",
                "result.trimWeightShare");
        assertContainsAll(source,
                "RewindWeightCalculator.crossSectionArea(",
                "RewindWeightCalculator.inchToMm(BigDecimal.valueOf(outDiameter))",
                "RewindWeightCalculator.inchToMm(BigDecimal.valueOf(coreDiameter))");
        assertContainsAll(save,
                "FinishPreviewVO preview = buildRewindPreview(orderUuid, roll, previewDto)",
                "spec.setEstimateWeight(finish.getEstimateWeight())");
        assertTrue(!source.contains("calcTrimWeightShare("),
                "rewind preview must not keep a second inline trim allocation formula");
        assertTrue(!source.contains("BigDecimal.valueOf(25.4)"),
                "process order service must not duplicate inch-to-mm conversion");
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        assertTrue(startIndex >= 0, "Missing start marker: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(endIndex >= 0, "Missing end marker: " + end);
        return source.substring(startIndex, endIndex);
    }

    private void assertContainsAll(String text, String... snippets) {
        for (String snippet : snippets) {
            assertTrue(text.contains(snippet), "Missing snippet: " + snippet);
        }
    }
}
