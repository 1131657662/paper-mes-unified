package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SawPlanCalculatorTest {

    private final SawPlanCalculator calculator = new SawPlanCalculator();

    @Test
    void calculate_loss_keeps_customer_widths_without_physical_remainder() {
        SawPlanCalculation result = calculator.calculate(specs(1000, 700), roll(1703, "1703"), "LOSS");

        assertEquals(WidthDifferencePolicy.LOSS, result.policy());
        assertEquals(3, result.differenceWidth());
        assertEquals(new BigDecimal("3.000"), result.differenceWeight());
        assertEquals(new BigDecimal("1700.000"), finishWeight(result));
    }

    @Test
    void calculate_allocate_assigns_all_input_weight_to_formal_outputs() {
        SawPlanCalculation result = calculator.calculate(specs(1000, 700), roll(1703, "1703"), "ALLOCATE");

        assertEquals(3, result.differenceWidth());
        assertEquals(new BigDecimal("1703.000"), finishWeight(result));
    }

    @Test
    void calculate_remainder_requires_explicit_trim_to_match_difference() {
        List<FinishConfigSpecDTO> specs = specs(1000, 700);
        specs.add(spec("TRIM", 2));

        assertThrows(BusinessException.class,
                () -> calculator.calculate(specs, roll(1703, "1703"), "REMAINDER"));
    }

    @Test
    void calculate_missing_policy_defaults_to_allocate() {
        SawPlanCalculation result = calculator.calculate(specs(1000, 700), roll(1703, "1703"), null);

        assertEquals(WidthDifferencePolicy.ALLOCATE, result.policy());
        assertEquals(new BigDecimal("1703.000"), finishWeight(result));
    }

    private BigDecimal finishWeight(SawPlanCalculation result) {
        return result.finishes().stream().map(SawPlanCalculation.CalculatedFinish::estimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<FinishConfigSpecDTO> specs(int first, int second) {
        return new java.util.ArrayList<>(List.of(spec("FINISH", first), spec("FINISH", second)));
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
