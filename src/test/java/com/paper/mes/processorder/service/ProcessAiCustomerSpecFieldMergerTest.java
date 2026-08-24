package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiCustomerSpecFieldMergerTest {

    private final ProcessAiPlanFieldMerger merger = new ProcessAiPlanFieldMerger();

    @Test
    void mergeCustomerPaperNameLeavesUnselectedSawSalesFieldsUnchanged() {
        FinishConfigSpecDTO currentFinish = finish(800);
        currentFinish.setCustomerGramWeight(200);
        currentFinish.setCustomerFinishWidth(900);
        currentFinish.setCustomerSpecOverrideReason("人工填写");
        FinishConfigSpecDTO proposedFinish = finish(800);
        proposedFinish.setCustomerPaperName("客户白卡");
        proposedFinish.setCustomerGramWeight(250);
        proposedFinish.setCustomerFinishWidth(800);
        proposedFinish.setCustomerSpecOverrideReason("合同要求");

        ProcessPlanDTO result = merger.merge(planWithFinish(currentFinish), candidate(planWithFinish(proposedFinish)),
                List.of("/assignments/R1/customerSpecs/0/paperName"));

        assertThat(result.getFinishSpecs()).singleElement().satisfies(spec -> {
            assertThat(spec.getCustomerPaperName()).isEqualTo("客户白卡");
            assertThat(spec.getCustomerGramWeight()).isEqualTo(200);
            assertThat(spec.getCustomerFinishWidth()).isEqualTo(900);
            assertThat(spec.getCustomerSpecOverrideReason()).isEqualTo("人工填写");
        });
    }

    @Test
    void mergeCustomerGramWeightLeavesUnselectedRewindSalesFieldsUnchanged() {
        RewindLayoutItemPlanDTO currentItem = layoutItem(800);
        currentItem.setCustomerPaperName("人工品名");
        currentItem.setCustomerFinishWidth(900);
        RewindLayoutItemPlanDTO proposedItem = layoutItem(800);
        proposedItem.setCustomerPaperName("客户白卡");
        proposedItem.setCustomerGramWeight(250);
        proposedItem.setCustomerFinishWidth(800);
        proposedItem.setCustomerSpecOverrideReason("合同要求");

        ProcessPlanDTO result = merger.merge(rewindPlan(currentItem), candidate(rewindPlan(proposedItem)),
                List.of("/assignments/R1/customerSpecs/0/gramWeight"));

        assertThat(result.getSegments()).singleElement().satisfies(segment ->
                assertThat(segment.getLayoutItems()).singleElement().satisfies(item -> {
                    assertThat(item.getCustomerPaperName()).isEqualTo("人工品名");
                    assertThat(item.getCustomerGramWeight()).isEqualTo(250);
                    assertThat(item.getCustomerFinishWidth()).isEqualTo(900);
                }));
    }

    @Test
    void mergeNewRewindPlanCopiesEachAcceptedCustomerSalesField() {
        RewindLayoutItemPlanDTO proposedItem = layoutItem(800);
        proposedItem.setCustomerPaperName("客户白卡");
        proposedItem.setCustomerGramWeight(250);
        proposedItem.setCustomerFinishWidth(800);
        proposedItem.setCustomerSpecOverrideReason("合同要求");

        ProcessPlanDTO result = merger.merge(null, candidate(rewindPlan(proposedItem)), List.of(
                "/assignments/R1/processType", "/assignments/R1/sourceRollRefs",
                "/assignments/R1/rewindIntent/widthRule/values",
                "/assignments/R1/customerSpecs/0/paperName",
                "/assignments/R1/customerSpecs/0/gramWeight",
                "/assignments/R1/customerSpecs/0/finishWidth",
                "/assignments/R1/customerSpecs/0/overrideReason"));

        assertThat(result.getSegments()).singleElement().satisfies(segment ->
                assertThat(segment.getLayoutItems()).singleElement().satisfies(item -> {
                    assertThat(item.getCustomerPaperName()).isEqualTo("客户白卡");
                    assertThat(item.getCustomerGramWeight()).isEqualTo(250);
                    assertThat(item.getCustomerFinishWidth()).isEqualTo(800);
                    assertThat(item.getCustomerSpecOverrideReason()).isEqualTo("合同要求");
                }));
    }

    @Test
    void mergeNewRewindPlanDoesNotCopyUnselectedCustomerSalesFields() {
        RewindLayoutItemPlanDTO proposedItem = layoutItem(800);
        proposedItem.setCustomerPaperName("客户白卡");
        proposedItem.setCustomerGramWeight(250);
        proposedItem.setCustomerFinishWidth(800);

        ProcessPlanDTO result = merger.merge(null, candidate(rewindPlan(proposedItem)), List.of(
                "/assignments/R1/processType", "/assignments/R1/sourceRollRefs",
                "/assignments/R1/rewindIntent/widthRule/values"));

        assertThat(result.getSegments()).singleElement().satisfies(segment ->
                assertThat(segment.getLayoutItems()).singleElement().satisfies(item -> {
                    assertThat(item.getCustomerPaperName()).isNull();
                    assertThat(item.getCustomerGramWeight()).isNull();
                    assertThat(item.getCustomerFinishWidth()).isNull();
                }));
    }

    private ProcessAiCompiledPlan candidate(ProcessPlanDTO plan) {
        return new ProcessAiCompiledPlan("R1", "roll-1", List.of(), plan, null);
    }

    private ProcessPlanDTO planWithFinish(FinishConfigSpecDTO finish) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setFinishSpecs(List.of(finish));
        return plan;
    }

    private ProcessPlanDTO rewindPlan(RewindLayoutItemPlanDTO item) {
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentSort(1);
        segment.setSegmentRatio(BigDecimal.valueOf(100));
        segment.setTargetDiameter(1600);
        segment.setFinishCoreDiameter(3);
        segment.setRepeatCount(1);
        segment.setLayoutItems(List.of(item));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(2);
        plan.setRewindMode(1);
        plan.setSegments(List.of(segment));
        return plan;
    }

    private FinishConfigSpecDTO finish(int width) {
        FinishConfigSpecDTO value = new FinishConfigSpecDTO();
        value.setItemType("FINISH");
        value.setFinishWidth(width);
        value.setCount(1);
        return value;
    }

    private RewindLayoutItemPlanDTO layoutItem(int width) {
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setItemType("FINISH");
        item.setWidth(width);
        item.setQuantity(1);
        return item;
    }
}
