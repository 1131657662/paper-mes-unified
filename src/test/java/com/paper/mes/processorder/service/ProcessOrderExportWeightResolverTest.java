package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderExportWeightResolverTest {

    @Test
    void fallbackWeightsReserveMeasuredTrimBeforeExportingProducts() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO product = finish("product", 900, false);
        ProcessOrderDetailVO.FinishProductionVO trim = finish("trim", 100, true);
        trim.setActualWeight(new BigDecimal("20"));
        production.setFinishes(List.of(product, trim));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("product", new BigDecimal("980"))
                .containsEntry("trim", new BigDecimal("20"));
    }

    @Test
    void fallbackWeightsKeepSavedRepeatedRewindPlansWithoutFlatteningTrimWidths() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        production.setRollWeight(new BigDecimal("2403"));
        production.setOriginalWidth(1625);
        ProcessOrderDetailVO.FinishProductionVO productA = savedFinish("product-a", "F001", 1550, false, "1146");
        ProcessOrderDetailVO.FinishProductionVO trimA = savedFinish("trim-a", "F002", 75, true, "56");
        ProcessOrderDetailVO.FinishProductionVO productB = savedFinish("product-b", "F003", 1550, false, "1146");
        ProcessOrderDetailVO.FinishProductionVO trimB = savedFinish("trim-b", "F004", 75, true, "55");
        production.setFinishes(List.of(productA, trimA, productB, trimB));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("product-a", new BigDecimal("1146"))
                .containsEntry("trim-a", new BigDecimal("56"))
                .containsEntry("product-b", new BigDecimal("1146"))
                .containsEntry("trim-b", new BigDecimal("55"));
    }

    @Test
    void fallbackWeightsKeepMeasuredProductAndAllocateOnlyTheUnmeasuredRemainder() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO measured = finish("measured", 500, false);
        measured.setActualWeight(new BigDecimal("700"));
        ProcessOrderDetailVO.FinishProductionVO unknown = finish("unknown", 500, false);
        production.setFinishes(List.of(measured, unknown));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("measured", new BigDecimal("700"))
                .containsEntry("unknown", new BigDecimal("300"));
    }

    @Test
    void fallbackWeightsDoNotHideUnresolvedTrimInsideProducts() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO product = finish("product", 1000, false);
        ProcessOrderDetailVO.FinishProductionVO trim = finish("trim", 0, true);
        trim.setFinishWidth(null);
        production.setFinishes(List.of(product, trim));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("product", null)
                .containsEntry("trim", null);
    }

    @Test
    void fallbackWeightsAllocateAGroupContainingOnlyTrim() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO trim = finish("trim-only", 1000, true);
        production.setFinishes(List.of(trim));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("trim-only", new BigDecimal("1000"));
    }

    @Test
    void fallbackWeightsUseAllocateWidthBudgetInsteadOfLegacyTrimEstimate() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        production.setOriginalWidth(1000);
        ProcessOrderDetailVO.FinishProductionVO product = finish("product", 600, false);
        ProcessOrderDetailVO.FinishProductionVO trim = finish("trim", 100, true);
        trim.setEstimateWeight(new BigDecimal("400"));
        production.setFinishes(List.of(product, trim));
        ProcessStep allocate = new ProcessStep();
        allocate.setStepType(1);
        allocate.setWidthDifferencePolicy("ALLOCATE");
        production.setSteps(List.of(allocate));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("product", new BigDecimal("900"))
                .containsEntry("trim", new BigDecimal("100"));
    }

    @Test
    void fallbackWeightsDoNotRoundAFractionalMeasuredRemainderIntoAFalseClosure() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO measured = finish("measured", 500, false);
        measured.setActualWeight(new BigDecimal("333.4"));
        ProcessOrderDetailVO.FinishProductionVO unknown = finish("unknown", 500, false);
        production.setFinishes(List.of(measured, unknown));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("measured", new BigDecimal("333"))
                .containsEntry("unknown", null);
    }

    @Test
    void referencedSourceWithZeroWeightDoesNotFallbackToUnrelatedProductionWeight() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO finish = finish("finish", 1000, false);
        ProcessOrderDetailVO.FinishSourceVO source = new ProcessOrderDetailVO.FinishSourceVO();
        source.setOriginalUuid("missing-weight-source");
        finish.setSources(List.of(source));
        production.setFinishes(List.of(finish));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("finish", null);
    }

    @Test
    void legacyRelationWithoutConsumptionRatioUsesFullSourceWeight() {
        ProcessOrderDetailVO.RollProductionVO production = production();
        ProcessOrderDetailVO.FinishProductionVO partial = finish("partial", 500, false);
        ProcessOrderDetailVO.FinishSourceVO partialSource = new ProcessOrderDetailVO.FinishSourceVO();
        partialSource.setOriginalUuid("source-1");
        partialSource.setConsumeRatio(new BigDecimal("50"));
        partial.setSources(List.of(partialSource));
        ProcessOrderDetailVO.FinishProductionVO legacy = finish("legacy", 500, false);
        ProcessOrderDetailVO.FinishSourceVO legacySource = new ProcessOrderDetailVO.FinishSourceVO();
        legacySource.setOriginalUuid("source-1");
        legacy.setSources(List.of(legacySource));
        production.setFinishes(List.of(partial, legacy));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(production));

        assertThat(weights).containsEntry("partial", new BigDecimal("500"))
                .containsEntry("legacy", new BigDecimal("500"));
    }

    @Test
    void legacyRelationAcrossDifferentSourceGroupsUsesOnlyRemainingSourceWeight() {
        ProcessOrderDetailVO.RollProductionVO first = production();
        ProcessOrderDetailVO.RollProductionVO second = production();
        second.setOriginalUuid("source-2");
        ProcessOrderDetailVO.FinishProductionVO partial = finish("partial", 500, false);
        ProcessOrderDetailVO.FinishSourceVO partialSource = new ProcessOrderDetailVO.FinishSourceVO();
        partialSource.setOriginalUuid("source-1");
        partialSource.setConsumeRatio(new BigDecimal("50"));
        partial.setSources(List.of(partialSource));
        ProcessOrderDetailVO.FinishProductionVO merged = finish("merged", 500, false);
        ProcessOrderDetailVO.FinishSourceVO legacySource = new ProcessOrderDetailVO.FinishSourceVO();
        legacySource.setOriginalUuid("source-1");
        ProcessOrderDetailVO.FinishSourceVO secondSource = new ProcessOrderDetailVO.FinishSourceVO();
        secondSource.setOriginalUuid("source-2");
        secondSource.setConsumeRatio(new BigDecimal("100"));
        merged.setSources(List.of(legacySource, secondSource));
        first.setFinishes(List.of(partial, merged));

        Map<String, BigDecimal> weights = ProcessOrderExportWeightResolver
                .fallbackEstimateWeights(List.of(first, second));

        assertThat(weights).containsEntry("partial", new BigDecimal("500"))
                .containsEntry("merged", new BigDecimal("1500"));
    }

    private ProcessOrderDetailVO.RollProductionVO production() {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setOriginalUuid("source-1");
        production.setRollWeight(new BigDecimal("1000"));
        production.setPieceNum(1);
        production.setOriginalWidth(1000);
        production.setMainStepType(1);
        return production;
    }

    private ProcessOrderDetailVO.FinishProductionVO finish(String uuid, int width, boolean trim) {
        ProcessOrderDetailVO.FinishProductionVO finish = new ProcessOrderDetailVO.FinishProductionVO();
        finish.setUuid(uuid);
        finish.setFinishWidth(width);
        finish.setIsRemain(trim ? 1 : 0);
        return finish;
    }

    private ProcessOrderDetailVO.FinishProductionVO savedFinish(String uuid, String rollNo, int width,
                                                                 boolean trim, String weight) {
        ProcessOrderDetailVO.FinishProductionVO finish = finish(uuid, width, trim);
        finish.setFinishRollNo(rollNo);
        finish.setEstimateWeight(new BigDecimal(weight));
        return finish;
    }
}
