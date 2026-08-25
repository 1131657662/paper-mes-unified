package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessOrderListStatsTest {

    @Test
    void apply_excludesSpareTrimAndVoidedFinishesFromFormalTotals() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll first = original(2, "100", null);
        OriginalRoll second = original(null, "90", "80");
        FinishRoll actual = finish(0, 0, 1, "50", "45");
        FinishRoll estimated = finish(null, null, null, "55", null);
        FinishRoll spare = finish(1, 0, 1, "10", null);
        FinishRoll trim = finish(0, 1, 1, "5", null);
        FinishRoll voided = finish(0, 0, 3, "20", "20");

        ProcessOrderListStats.apply(
                order,
                List.of(first, second),
                List.of(actual, estimated, spare, trim, voided));

        assertEquals(2, order.getOriginalRollCount());
        assertEquals(3, order.getOriginalPieceCount());
        assertEquals(new BigDecimal("180"), order.getOriginalRollWeight());
        assertEquals(2, order.getFinishRollCount());
        assertEquals(new BigDecimal("100"), order.getFinishRollWeight());
        assertEquals(new BigDecimal("105"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("45"), order.getActualFinishWeight());
        assertEquals(1, order.getSpareRollCount());
    }

    @Test
    void apply_excludesScrappedFinishesFromFormalAndSpareTotals() {
        ProcessOrder order = new ProcessOrder();
        FinishRoll formal = finish(0, 0, 1, "20", "15");
        formal.setFinishStatus(4);
        FinishRoll spare = finish(1, 0, 1, "10", null);
        spare.setFinishStatus(4);

        ProcessOrderListStats.apply(order, List.of(), List.of(formal, spare));

        assertEquals(0, order.getFinishRollCount());
        assertEquals(BigDecimal.ZERO, order.getFinishRollWeight());
        assertEquals(BigDecimal.ZERO, order.getEstimateFinishWeight());
        assertEquals(BigDecimal.ZERO, order.getActualFinishWeight());
        assertEquals(0, order.getSpareRollCount());
    }

    @Test
    void apply_excludesDisposedAndDirectOriginalRollsFromProductionTotals() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll active = original(1, "100", "90");
        OriginalRoll cancelled = original(1, "200", "180");
        cancelled.setDispositionAction(ProcessRollDispositionAction.CANCEL);
        OriginalRoll direct = original(1, "300", "270");
        direct.setProcessMode(3);

        ProcessOrderListStats.apply(order, List.of(active, cancelled, direct), List.of());

        assertEquals(1, order.getOriginalRollCount());
        assertEquals(1, order.getOriginalPieceCount());
        assertEquals(new BigDecimal("90"), order.getOriginalRollWeight());
    }

    @Test
    void apply_fallsBackToNominalTotalWhenLegacyTotalWeightIsMissing() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll roll = original(3, null, null);
        roll.setRollWeight(new BigDecimal("620"));

        ProcessOrderListStats.apply(order, List.of(roll), List.of());

        assertEquals(new BigDecimal("1860"), order.getOriginalRollWeight());
    }

    @Test
    void apply_rebalancesLegacySawEstimatesToMeasuredMotherWeight() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, null, "1862");
        source.setUuid("source-1");
        source.setOriginalWidth(2400);
        source.setMainStepType(1);
        List<FinishRoll> finishes = List.of(
                sawFinish("finish-a", source, "621"),
                sawFinish("finish-b", source, "621"),
                sawFinish("finish-c", source, "621"));

        ProcessOrderListStats.apply(order, List.of(source), finishes);

        assertEquals(new BigDecimal("1862"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("1862"), order.getFinishRollWeight());
    }

    @Test
    void apply_keepsSavedRepeatedRewindPlansWithoutFlatteningTrimWidths() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, null, "2403");
        source.setUuid("source-1");
        source.setOriginalWidth(1625);
        source.setMainStepType(2);
        FinishRoll productA = savedFinish("product-a", source, 1550, false, "1146");
        FinishRoll trimA = savedFinish("trim-a", source, 75, true, "56");
        FinishRoll productB = savedFinish("product-b", source, 1550, false, "1146");
        FinishRoll trimB = savedFinish("trim-b", source, 75, true, "55");

        ProcessOrderListStats.apply(order, List.of(source), List.of(productA, trimA, productB, trimB));

        assertEquals(new BigDecimal("2292"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("2292"), order.getFinishRollWeight());
    }

    @Test
    void apply_reservesTrimFromMotherWeightBeforeSubtractingPlannedLoss() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, "800", null);
        source.setUuid("source-1");
        source.setOriginalWidth(1500);
        source.setMainStepType(1);
        FinishRoll product = sawFinish("finish-product", source, null);
        product.setFinishWidth(1400);
        FinishRoll trim = finish(0, 1, 1, null, null);
        trim.setUuid("finish-trim");
        trim.setOriginalRollNos(source.getUuid());
        trim.setFinishWidth(80);
        ProcessStep loss = new ProcessStep();
        loss.setUuid("step-loss");
        loss.setOriginalUuid(source.getUuid());
        loss.setWidthDifferencePolicy("LOSS");
        loss.setPlannedLossWeight(new BigDecimal("11"));

        ProcessOrderListStats.apply(order, List.of(source), List.of(product, trim), List.of(), List.of(loss));

        assertEquals(new BigDecimal("746"), order.getEstimateFinishWeight());
    }

    @Test
    void apply_reservesMeasuredTrimBeforeEstimatingFormalProduct() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, "800", null);
        source.setUuid("source-1");
        source.setOriginalWidth(1500);
        source.setMainStepType(1);
        FinishRoll product = sawFinish("finish-product", source, null);
        product.setFinishWidth(1400);
        FinishRoll trim = finish(0, 1, 1, null, "20");
        trim.setUuid("finish-trim");
        trim.setOriginalRollNos(source.getUuid());
        trim.setFinishWidth(80);
        ProcessStep loss = new ProcessStep();
        loss.setUuid("step-loss");
        loss.setOriginalUuid(source.getUuid());
        loss.setWidthDifferencePolicy("LOSS");
        loss.setPlannedLossWeight(new BigDecimal("11"));

        ProcessOrderListStats.apply(order, List.of(source), List.of(product, trim), List.of(), List.of(loss));

        assertEquals(new BigDecimal("769"), order.getEstimateFinishWeight());
    }

    @Test
    void apply_keepsMeasuredTrimAndAllocatesTheRemainingUnknownTrimBudget() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, "1000", null);
        source.setUuid("source-1");
        source.setOriginalWidth(1000);
        source.setMainStepType(1);
        FinishRoll product = sawFinish("finish-product", source, null);
        product.setFinishWidth(500);
        FinishRoll measuredTrim = finish(0, 1, 1, null, "20");
        measuredTrim.setUuid("finish-measured-trim");
        measuredTrim.setOriginalRollNos(source.getUuid());
        measuredTrim.setFinishWidth(100);
        FinishRoll unknownTrim = finish(0, 1, 1, null, null);
        unknownTrim.setUuid("finish-unknown-trim");
        unknownTrim.setOriginalRollNos(source.getUuid());
        unknownTrim.setFinishWidth(100);

        ProcessOrderListStats.apply(order, List.of(source),
                List.of(product, measuredTrim, unknownTrim));

        assertEquals(new BigDecimal("500"), order.getEstimateFinishWeight());
    }

    @Test
    void apply_locksMeasuredProductAndAllocatesOnlyRemainingMotherWeight() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, null, "1862");
        source.setUuid("source-1");
        source.setOriginalWidth(2400);
        source.setMainStepType(1);
        FinishRoll measured = sawFinish("finish-measured", source, null);
        measured.setActualWeight(new BigDecimal("700"));
        FinishRoll unknown = sawFinish("finish-unknown", source, null);

        ProcessOrderListStats.apply(order, List.of(source), List.of(measured, unknown));

        assertEquals(new BigDecimal("1862"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("1862"), order.getFinishRollWeight());
    }

    @Test
    void apply_doesNotHideUnknownTrimWeightInsideFormalProducts() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, null, "1000");
        source.setUuid("source-1");
        source.setOriginalWidth(2400);
        source.setMainStepType(1);
        FinishRoll product = sawFinish("finish-product", source, null);
        FinishRoll trim = finish(0, 1, 1, null, null);
        trim.setUuid("finish-trim");
        trim.setOriginalRollNos(source.getUuid());

        ProcessOrderListStats.apply(order, List.of(source), List.of(product, trim));

        assertEquals(BigDecimal.ZERO, order.getEstimateFinishWeight());
    }

    @Test
    void apply_doesNotUseUnknownMotherWeightAsEstimateSource() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll source = original(1, "1862", null);
        source.setUuid("unknown-source");
        source.setWeightStatus("UNKNOWN");

        ProcessOrderListStats.apply(order, List.of(source), List.of());

        assertEquals(BigDecimal.ZERO, order.getOriginalRollWeight());
    }

    @Test
    void apply_allocatesMergedFinishFromAllRelatedMotherRolls() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll first = original(1, "100", null);
        first.setUuid("source-1");
        first.setMainStepType(2);
        OriginalRoll second = original(1, "200", null);
        second.setUuid("source-2");
        second.setMainStepType(2);
        FinishRoll left = finish(0, 0, 1, "10", null);
        left.setUuid("finish-left");
        FinishRoll right = finish(0, 0, 1, "20", null);
        right.setUuid("finish-right");
        FinishOriginalRel leftFirst = relation("finish-left", "source-1");
        FinishOriginalRel leftSecond = relation("finish-left", "source-2");
        FinishOriginalRel rightFirst = relation("finish-right", "source-1");
        FinishOriginalRel rightSecond = relation("finish-right", "source-2");

        ProcessOrderListStats.apply(order, List.of(first, second), List.of(left, right),
                List.of(leftFirst, leftSecond, rightFirst, rightSecond));

        assertEquals(new BigDecimal("300"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("300"), order.getFinishRollWeight());
    }

    @Test
    void apply_legacyRelationAcrossGroupsUsesOnlyRemainingSourceWeight() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll first = original(1, "1000", null);
        first.setUuid("source-1");
        first.setMainStepType(2);
        OriginalRoll second = original(1, "1000", null);
        second.setUuid("source-2");
        second.setMainStepType(2);
        FinishRoll partial = finish(0, 0, 1, "10", null);
        partial.setUuid("finish-partial");
        FinishRoll merged = finish(0, 0, 1, "20", null);
        merged.setUuid("finish-merged");
        FinishOriginalRel partialSource = relation("finish-partial", "source-1");
        partialSource.setConsumeRatio(new BigDecimal("50"));
        FinishOriginalRel legacySource = relation("finish-merged", "source-1");
        FinishOriginalRel secondSource = relation("finish-merged", "source-2");
        secondSource.setConsumeRatio(new BigDecimal("100"));

        ProcessOrderListStats.apply(order, List.of(first, second), List.of(partial, merged),
                List.of(partialSource, legacySource, secondSource));

        assertEquals(new BigDecimal("2000"), order.getEstimateFinishWeight());
        assertEquals(new BigDecimal("2000"), order.getFinishRollWeight());
    }

    @Test
    void apply_blocksMergedEstimateWhenAnyMotherWeightIsUnknown() {
        ProcessOrder order = new ProcessOrder();
        OriginalRoll known = original(1, "100", null);
        known.setUuid("source-known");
        OriginalRoll unknown = original(1, null, null);
        unknown.setUuid("source-unknown");
        unknown.setWeightStatus("UNKNOWN");
        FinishRoll finish = finish(0, 0, 1, "50", null);
        finish.setUuid("finish-merged");
        ProcessOrderListStats.apply(order, List.of(known, unknown), List.of(finish),
                List.of(relation("finish-merged", "source-known"),
                        relation("finish-merged", "source-unknown")));

        assertEquals(BigDecimal.ZERO, order.getEstimateFinishWeight());
        assertEquals(BigDecimal.ZERO, order.getFinishRollWeight());
    }

    private OriginalRoll original(Integer pieces, String totalWeight, String actualWeight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setPieceNum(pieces);
        roll.setTotalWeight(decimal(totalWeight));
        roll.setActualWeight(decimal(actualWeight));
        return roll;
    }

    private FinishRoll finish(
            Integer spare,
            Integer remain,
            Integer rollNoStatus,
            String estimateWeight,
            String actualWeight) {
        FinishRoll roll = new FinishRoll();
        roll.setIsSpare(spare);
        roll.setIsRemain(remain);
        roll.setRollNoStatus(rollNoStatus);
        roll.setEstimateWeight(decimal(estimateWeight));
        roll.setActualWeight(decimal(actualWeight));
        return roll;
    }

    private FinishRoll sawFinish(String uuid, OriginalRoll source, String estimateWeight) {
        FinishRoll finish = finish(0, 0, 1, estimateWeight, null);
        finish.setUuid(uuid);
        finish.setOriginalRollNos(source.getUuid());
        finish.setFinishWidth(800);
        return finish;
    }

    private FinishRoll savedFinish(String uuid, OriginalRoll source, int width, boolean trim, String estimateWeight) {
        FinishRoll finish = finish(0, trim ? 1 : 0, 1, estimateWeight, null);
        finish.setUuid(uuid);
        finish.setFinishRollNo(uuid);
        finish.setOriginalRollNos(source.getUuid());
        finish.setFinishWidth(width);
        return finish;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private FinishOriginalRel relation(String finishUuid, String originalUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(originalUuid);
        return relation;
    }
}
