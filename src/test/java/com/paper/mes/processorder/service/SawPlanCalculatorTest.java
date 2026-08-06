package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SawPlanCalculatorTest {

    private final SawPlanCalculator calculator = new SawPlanCalculator();

    @Test
    void loss_countsOnlyUnassignedWidthAsLoss() {
        SawPlanCalculation result = calculator.calculate(
                List.of(spec("FINISH", 2200)), roll(2300, "2300"), "LOSS");

        assertEquals(WidthDifferencePolicy.LOSS, result.policy());
        assertEquals(100, result.differenceWidth());
        assertEquals(new BigDecimal("100.000"), result.differenceWeight());
        assertEquals(new BigDecimal("2200.000"), finishWeight(result));
        assertEquals(BigDecimal.ZERO, trimWeight(result));
    }

    @Test
    void loss_preservesExplicitTrimAndLosesOnlyTheRemainingGap() {
        SawPlanCalculation result = calculator.calculate(List.of(
                spec("FINISH", 2000), spec("TRIM", 290)), roll(2300, "2300"), "LOSS");

        assertEquals(10, result.differenceWidth());
        assertEquals(new BigDecimal("10.000"), result.differenceWeight());
        assertEquals(new BigDecimal("2000.000"), finishWeight(result));
        assertEquals(new BigDecimal("290.000"), trimWeight(result));
    }

    @Test
    void allocate_splitsTheGapEvenlyAcrossConfiguredPieces() {
        SawPlanCalculation result = calculator.calculate(
                List.of(spec("FINISH", 1000), spec("FINISH", 1200)),
                roll(2400, "2400"), "ALLOCATE");

        assertEquals(200, result.differenceWidth());
        assertEquals(List.of(new BigDecimal("1100.000"), new BigDecimal("1300.000")),
                result.finishes().stream().map(SawPlanCalculation.CalculatedFinish::estimateWeight).toList());
        assertEquals(new BigDecimal("2400.000"), finishWeight(result));
    }

    @Test
    void remainder_requiresAllSourceWidthToBeRepresented() {
        List<FinishConfigSpecDTO> incomplete = new ArrayList<>(List.of(
                spec("FINISH", 2300), spec("TRIM", 10)));

        assertThrows(BusinessException.class,
                () -> calculator.calculate(incomplete, roll(2400, "2400"), "REMAINDER"));

        incomplete.set(1, spec("TRIM", 100));
        SawPlanCalculation complete = calculator.calculate(
                incomplete, roll(2400, "2400"), "REMAINDER");
        assertEquals(0, complete.differenceWidth());
        assertEquals(new BigDecimal("2300.000"), finishWeight(complete));
        assertEquals(new BigDecimal("100.000"), trimWeight(complete));
    }

    @Test
    void missingPolicyDefaultsToRemainder() {
        assertThrows(BusinessException.class, () -> calculator.calculate(
                List.of(spec("FINISH", 2300)), roll(2400, "2400"), null));
    }

    @Test
    void roundingRemainderIsAppliedToLastFinishInsteadOfTrim() {
        SawPlanCalculation result = calculator.calculate(List.of(
                spec("FINISH", 1175), spec("FINISH", 1175), spec("TRIM", 3)),
                roll(2353, "2285"), "REMAINDER");

        assertEquals(new BigDecimal("2282.087"), finishWeight(result));
        assertEquals(new BigDecimal("2.913"), trimWeight(result));
    }

    @Test
    void multipleSourcePieces_expandEachPhysicalOutputWithPerPieceWeight() {
        OriginalRoll roll = roll(1000, "744.900");
        roll.setPieceNum(8);

        SawPlanCalculation result = calculator.calculate(List.of(
                spec("FINISH", 900), spec("TRIM", 100)), roll, "REMAINDER");

        assertEquals(8, result.finishes().size());
        assertEquals(8, result.trims().size());
        assertEquals(new BigDecimal("670.410"), result.finishes().getFirst().estimateWeight());
        assertEquals(new BigDecimal("74.490"), result.trims().getFirst().estimateWeight());
        assertEquals(new BigDecimal("5363.280"), finishWeight(result));
        assertEquals(new BigDecimal("595.920"), trimWeight(result));
        assertEquals(8, result.knifeCount());
    }

    @Test
    void sourcePieceExpansion_overLimit_isRejected() {
        OriginalRoll roll = roll(1000, "744.900");
        roll.setPieceNum(501);

        assertThrows(BusinessException.class, () -> calculator.calculate(
                List.of(spec("FINISH", 1000)), roll, "REMAINDER"));
    }

    private BigDecimal finishWeight(SawPlanCalculation result) {
        return result.finishes().stream().map(SawPlanCalculation.CalculatedFinish::estimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal trimWeight(SawPlanCalculation result) {
        return result.trims().stream().map(SawPlanCalculation.CalculatedFinish::estimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FinishConfigSpecDTO spec(String type, int width) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(type);
        spec.setFinishWidth(width);
        spec.setCount(1);
        return spec;
    }

    private OriginalRoll roll(int width, String weight) {
        OriginalRoll roll = new OriginalRoll();
        roll.setOriginalWidth(width);
        roll.setRollWeight(new BigDecimal(weight));
        roll.setPieceNum(1);
        return roll;
    }
}
