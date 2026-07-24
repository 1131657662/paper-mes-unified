package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettleFeeLineBuilderTest {

    @Test
    void fromSteps_serviceByPiece_buildsNamedServiceFeeLine() {
        ProcessStep step = new ProcessStep();
        step.setStepType(4);
        step.setStepName("重新包装");
        step.setBillingBasis("PIECE");
        step.setServiceQuantity(new BigDecimal("12"));
        step.setUnitPrice(new BigDecimal("8.5"));
        step.setStepAmount(new BigDecimal("102"));
        step.setStandardStepAmount(new BigDecimal("102"));

        List<SettleFeeLineVO> lines = SettleFeeLineBuilder.fromSteps(
                new SettlePrintLineVO(), new OriginalRoll(), List.of(step), List.of());

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.getFeeType()).isEqualTo("service");
            assertThat(line.getFeeName()).isEqualTo("重新包装");
            assertThat(line.getQuantity()).isEqualByComparingTo("12");
            assertThat(line.getQuantityUnit()).isEqualTo("件");
            assertThat(line.getAmountNoTax()).isEqualByComparingTo("102.00");
        });
    }
}
