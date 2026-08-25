package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderStageOutputExportWeightResolverTest {

    @Test
    void allocatesMotherWeightAsIntegerRemainderAcrossThreeEqualOutputs() {
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(output("a", 800, false), output("b", 800, false), output("c", 800, false)));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("a", decimal("621"))
                .containsEntry("b", decimal("621"))
                .containsEntry("c", decimal("620"));
    }

    @Test
    void allocatePolicyKeepsWidthDifferenceInSaleableOutputs() {
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(output("a", 800, false), output("b", 800, false)));
        production.setSteps(List.of(step("ALLOCATE", null)));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("a", decimal("931"))
                .containsEntry("b", decimal("931"));
    }

    @Test
    void allocatePolicyIgnoresStaleUnmeasuredTrimEstimateWhenWidthIsKnown() {
        ProcessOrderDetailVO.StageOutputVO product = output("product", 600, false);
        ProcessOrderDetailVO.StageOutputVO trim = output("trim", 100, true);
        trim.setEstimateWeight(decimal("400"));
        ProcessOrderDetailVO.RollProductionVO production = production("1000", 1000,
                outputs(product, trim));
        production.setSteps(List.of(step("ALLOCATE", null)));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("product", decimal("900"))
                .containsEntry("trim", decimal("100"));
    }

    @Test
    void lossPolicyRemovesPlannedLossBeforeOutputAllocation() {
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(output("a", 1200, false), output("b", 1200, false)));
        production.setSteps(List.of(step("LOSS", decimal("200"))));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("a", decimal("831"))
                .containsEntry("b", decimal("831"));
    }

    @Test
    void remainderPolicyReservesTrimBudgetBeforeProductAllocation() {
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(output("a", 800, false), output("b", 800, false), output("trim", 800, true)));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("a", decimal("621"))
                .containsEntry("b", decimal("620"))
                .containsEntry("trim", decimal("621"));
    }

    @Test
    void keepsSavedStageWeightsForRepeatedRewindLayouts() {
        ProcessOrderDetailVO.StageOutputVO productA = savedOutput("product-a", 1550, false, "1146");
        ProcessOrderDetailVO.StageOutputVO trimA = savedOutput("trim-a", 75, true, "56");
        ProcessOrderDetailVO.StageOutputVO productB = savedOutput("product-b", 1550, false, "1146");
        ProcessOrderDetailVO.StageOutputVO trimB = savedOutput("trim-b", 75, true, "55");
        ProcessOrderDetailVO.RollProductionVO production = production("2403", 1625,
                outputs(productA, trimA, productB, trimB));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("product-a", decimal("1146"))
                .containsEntry("trim-a", decimal("56"))
                .containsEntry("product-b", decimal("1146"))
                .containsEntry("trim-b", decimal("55"));
    }

    @Test
    void knownActualOutputIsLockedAndOnlyUnknownOutputsReceiveFallback() {
        ProcessOrderDetailVO.StageOutputVO actual = output("actual", 1200, false);
        actual.setActualWeight(decimal("700"));
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(actual, output("unknown", 1200, false)));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("unknown", decimal("1162"))
                .doesNotContainKey("actual");
    }

    @Test
    void measuredTrimIsReservedBeforeAllocatingUnknownProducts() {
        ProcessOrderDetailVO.StageOutputVO trim = output("trim", 800, true);
        trim.setActualWeight(decimal("100"));
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(output("a", 800, false), output("b", 800, false), trim));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("a", decimal("881"))
                .containsEntry("b", decimal("881"))
                .doesNotContainKey("trim");
    }

    @Test
    void measuredTrimAndUnknownTrimUseTheCompleteTrimBudget() {
        ProcessOrderDetailVO.StageOutputVO measuredTrim = output("measured-trim", 100, true);
        measuredTrim.setActualWeight(decimal("20"));
        ProcessOrderDetailVO.StageOutputVO unknownTrim = output("unknown-trim", 100, true);
        ProcessOrderDetailVO.RollProductionVO production = production("1000", 1000,
                outputs(output("product", 500, false), measuredTrim, unknownTrim));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("product", decimal("500"))
                .containsEntry("unknown-trim", decimal("480"));
    }

    @Test
    void fractionalMeasuredOutputDoesNotCreateAFalseIntegerFallback() {
        ProcessOrderDetailVO.StageOutputVO measured = output("measured", 1200, false);
        measured.setActualWeight(decimal("333.4"));
        ProcessOrderDetailVO.RollProductionVO production = production("1000", 2400,
                outputs(measured, output("unknown", 1200, false)));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("unknown", null);
    }

    @Test
    void unknownTrimWithoutWidthOrWeightBlocksProductFallback() {
        ProcessOrderDetailVO.StageOutputVO trim = output("trim", 0, true);
        trim.setFinishWidth(null);
        ProcessOrderDetailVO.RollProductionVO production = production("1000", 2400,
                outputs(output("product", 2400, false), trim));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("product", null).containsEntry("trim", null);
    }

    @Test
    void missingParentOutputBlocksStageFallbackInsteadOfPartiallyAllocating() {
        ProcessOrderDetailVO.StageOutputVO child = output("child", 800, false);
        child.setInputOutputUuids(List.of("missing-parent"));
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400, outputs(child));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("child", null);
    }

    @Test
    void unlinkedNextStageInheritsPreviousStageProductBudget() {
        ProcessOrderDetailVO.RollProductionVO production = production("1862", 2400,
                outputs(output("first-a", 1200, false), output("first-b", 1200, false),
                        output("next-a", 1200, false), output("next-b", 1200, false)));
        production.getStageOutputs().get(0).setStageLevel(1);
        production.getStageOutputs().get(1).setStageLevel(1);
        production.getStageOutputs().get(2).setStageLevel(2);
        production.getStageOutputs().get(3).setStageLevel(2);
        ProcessStep firstStep = step("LOSS", decimal("200"));
        firstStep.setStageLevel(1);
        ProcessStep nextStep = step("ALLOCATE", null);
        nextStep.setStageLevel(2);
        production.setSteps(List.of(firstStep, nextStep));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver.stageOutputEstimateWeights(production);

        assertThat(weights).containsEntry("first-a", decimal("831"))
                .containsEntry("first-b", decimal("831"))
                .containsEntry("next-a", decimal("831"))
                .containsEntry("next-b", decimal("831"));
    }

    private ProcessOrderDetailVO.RollProductionVO production(String weight, int width,
                                                              List<ProcessOrderDetailVO.StageOutputVO> outputs) {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setRollWeight(decimal(weight));
        production.setPieceNum(1);
        production.setOriginalWidth(width);
        production.setStageOutputs(outputs);
        return production;
    }

    private List<ProcessOrderDetailVO.StageOutputVO> outputs(ProcessOrderDetailVO.StageOutputVO... outputs) {
        return List.of(outputs);
    }

    private ProcessOrderDetailVO.StageOutputVO output(String uuid, int width, boolean trim) {
        ProcessOrderDetailVO.StageOutputVO output = new ProcessOrderDetailVO.StageOutputVO();
        output.setUuid(uuid);
        output.setStageLevel(1);
        output.setOutputSort(width);
        output.setFinishWidth(width);
        output.setIsRemain(trim ? 1 : 0);
        return output;
    }

    private ProcessOrderDetailVO.StageOutputVO savedOutput(String uuid, int width, boolean trim, String weight) {
        ProcessOrderDetailVO.StageOutputVO output = output(uuid, width, trim);
        output.setEstimateWeight(decimal(weight));
        output.setWeightStatus("ESTIMATED");
        return output;
    }

    private ProcessStep step(String policy, BigDecimal loss) {
        ProcessStep step = new ProcessStep();
        step.setStageLevel(1);
        step.setWidthDifferencePolicy(policy);
        step.setPlannedLossWeight(loss);
        return step;
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
