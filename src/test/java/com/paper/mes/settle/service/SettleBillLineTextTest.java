package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettleBillLineTextTest {

    @Test
    void originalWeightText_formatsUnknownEstimatedMeasuredAndLegacyStates() {
        assertThat(SettleBillLineText.originalWeightText(line("UNKNOWN", "1")))
                .isEqualTo("未知（待称重）");
        assertThat(SettleBillLineText.originalWeightText(line("ESTIMATED", "1")))
                .isEqualTo("参考 1 kg（未实测）");
        assertThat(SettleBillLineText.originalWeightText(line("MEASURED", "2000")))
                .isEqualTo("实测 2000 kg");
        assertThat(SettleBillLineText.originalWeightText(line(null, "3")))
                .isEqualTo("3 kg");
    }

    private SettlePrintLineVO line(String status, String weight) {
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setOriginalWeightStatus(status);
        line.setOriginalWeight(new BigDecimal(weight));
        return line;
    }
}
