package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RollLossSummaryCalculatorTest {

    @Test
    void calculate_whenStepScrapAndTrimExist_sumsAllLossSourcesByShareRatio() {
        Map<String, BigDecimal> result = RollLossSummaryCalculator.calculate(
                List.of(original("roll-a", 1)),
                List.of(step("roll-a", "2.500")),
                List.of(finish("finish-a", 1, 0, 1, "1.000", "0.500")),
                List.of(rel("finish-a", "roll-a", "50")));

        assertEquals(new BigDecimal("3.250"), result.get("roll-a").setScale(3));
    }

    @Test
    void calculate_whenOriginalIsDirectShip_ignoresAllLossSources() {
        Map<String, BigDecimal> result = RollLossSummaryCalculator.calculate(
                List.of(original("roll-a", 3)),
                List.of(step("roll-a", "2.000")),
                List.of(finish("finish-a", 1, 0, 1, "1.000", "1.000")),
                List.of(rel("finish-a", "roll-a", "100")));

        assertFalse(result.containsKey("roll-a"));
    }

    @Test
    void calculate_whenFinishIsDirectSpareOrVoided_ignoresFinishLoss() {
        Map<String, BigDecimal> result = RollLossSummaryCalculator.calculate(
                List.of(original("roll-a", 1)),
                List.of(),
                List.of(
                        finish("direct", 2, 0, 1, "1.000", "1.000"),
                        finish("spare", 1, 1, 1, "1.000", "1.000"),
                        finish("voided", 1, 0, 3, "1.000", "1.000")),
                List.of(
                        rel("direct", "roll-a", "100"),
                        rel("spare", "roll-a", "100"),
                        rel("voided", "roll-a", "100")));

        assertFalse(result.containsKey("roll-a"));
    }

    @Test
    void calculate_whenFinishIsRemain_countsRemainWeightAsLoss() {
        Map<String, BigDecimal> result = RollLossSummaryCalculator.calculate(
                List.of(original("roll-a", 1)),
                List.of(),
                List.of(remainFinish("trim-a", "4.000")),
                List.of(rel("trim-a", "roll-a", "25")));

        assertEquals(new BigDecimal("1.000"), result.get("roll-a").setScale(3));
    }

    private static OriginalRoll original(String uuid, int processMode) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setProcessMode(processMode);
        return roll;
    }

    private static ProcessStep step(String originalUuid, String lossWeight) {
        ProcessStep step = new ProcessStep();
        step.setOriginalUuid(originalUuid);
        step.setLossWeight(new BigDecimal(lossWeight));
        return step;
    }

    private static FinishRoll finish(String uuid, int sourceType, int isSpare, int rollNoStatus,
                                     String scrapWeight, String trimWeight) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setSourceType(sourceType);
        finish.setIsSpare(isSpare);
        finish.setRollNoStatus(rollNoStatus);
        finish.setScrapWeight(new BigDecimal(scrapWeight));
        finish.setTrimWeightShare(new BigDecimal(trimWeight));
        return finish;
    }

    private static FinishRoll remainFinish(String uuid, String actualWeight) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setSourceType(1);
        finish.setIsSpare(0);
        finish.setIsRemain(1);
        finish.setRollNoStatus(1);
        finish.setActualWeight(new BigDecimal(actualWeight));
        return finish;
    }

    private static FinishOriginalRel rel(String finishUuid, String originalUuid, String shareRatio) {
        FinishOriginalRel rel = new FinishOriginalRel();
        rel.setFinishUuid(finishUuid);
        rel.setOriginalUuid(originalUuid);
        rel.setShareRatio(new BigDecimal(shareRatio));
        return rel;
    }
}
