package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettleExportSubtotalTest {

    @Test
    void add_usesOrderFinishTotalsWithoutRepeatingSharedOutputs() {
        SettleExportSubtotal subtotal = new SettleExportSubtotal();

        subtotal.add(enrichedLine("1", "3"));
        subtotal.add(enrichedLine("1", "3"));
        subtotal.add(enrichedLine("1", "3"));

        assertThat(subtotal.finishCount).isEqualTo(1);
        assertThat(subtotal.finishWeight).isEqualByComparingTo("3");
    }

    @Test
    void add_legacyLinesStillUsePerLineFinishTotals() {
        SettleExportSubtotal subtotal = new SettleExportSubtotal();

        subtotal.add(legacyLine("1", "3"));
        subtotal.add(legacyLine("2", "4"));

        assertThat(subtotal.finishCount).isEqualTo(3);
        assertThat(subtotal.finishWeight).isEqualByComparingTo("7");
    }

    @Test
    void originalWeightText_whenAllWeightsAreEstimated_marksSubtotalAsReference() {
        SettleExportSubtotal subtotal = new SettleExportSubtotal();

        subtotal.add(weightLine("ESTIMATED", "1"));
        subtotal.add(weightLine("ESTIMATED", "2"));

        assertThat(subtotal.originalWeightText()).isEqualTo("参考 3 kg（未实测）");
    }

    @Test
    void originalWeightText_whenSomeWeightsAreUnknown_reportsOnlyKnownWeight() {
        SettleExportSubtotal subtotal = new SettleExportSubtotal();

        subtotal.add(weightLine("MEASURED", "2"));
        subtotal.add(weightLine("UNKNOWN", "99"));

        assertThat(subtotal.originalWeightText()).isEqualTo("已知 2 kg；1 卷待称重");
    }

    private SettlePrintLineVO enrichedLine(String count, String weight) {
        SettlePrintLineVO line = legacyLine(count, weight);
        line.setOrderFinishCount(Integer.valueOf(count));
        line.setOrderFinishWeight(new BigDecimal(weight));
        return line;
    }

    private SettlePrintLineVO legacyLine(String count, String weight) {
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setFinishCount(Integer.valueOf(count));
        line.setFinishWeight(new BigDecimal(weight));
        return line;
    }

    private SettlePrintLineVO weightLine(String status, String weight) {
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setOriginalWeightStatus(status);
        line.setOriginalWeight(new BigDecimal(weight));
        return line;
    }
}
