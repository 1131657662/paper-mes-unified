package com.paper.mes.processorder.calc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FeeCalculator 计费引擎单测（P1-5 / V4.1 §2.5/§2.6）。
 * 锯纸=刀数×单价、复卷=吨位×单价，逐工序四舍五入取整；含税/不含税整单装配。
 */
class FeeCalculatorTest {

    @Test
    void saw_amount_round() {
        // 7刀 × 12.50元/刀 = 87.5 → 取整 88。
        BigDecimal a = FeeCalculator.stepAmount(FeeCalculator.STEP_TYPE_SAW,
                7, null, new BigDecimal("12.50"));
        assertEquals(0, a.compareTo(new BigDecimal("88")), "锯纸费取整");
    }

    @Test
    void rewind_amount_round() {
        // 1.234吨 × 800元/吨 = 987.2 → 取整 987。
        BigDecimal a = FeeCalculator.stepAmount(FeeCalculator.STEP_TYPE_REWIND,
                null, new BigDecimal("1.234"), new BigDecimal("800"));
        assertEquals(0, a.compareTo(new BigDecimal("987")), "复卷费取整");
    }

    @Test
    void missing_price_or_qty_is_zero() {
        assertEquals(0, FeeCalculator.stepAmount(FeeCalculator.STEP_TYPE_SAW, 5, null, null)
                .compareTo(BigDecimal.ZERO), "无单价=0");
        assertEquals(0, FeeCalculator.stepAmount(FeeCalculator.STEP_TYPE_REWIND, null, null, new BigDecimal("800"))
                .compareTo(BigDecimal.ZERO), "无吨位=0");
        assertEquals(0, FeeCalculator.stepAmount(FeeCalculator.STEP_TYPE_SAW, 0, null, new BigDecimal("12.5"))
                .compareTo(BigDecimal.ZERO), "0刀=0");
    }

    @Test
    void tax_only_when_invoice() {
        // 开票 1000 × 13% = 130.00。
        assertEquals(0, FeeCalculator.tax(new BigDecimal("1000"), new BigDecimal("13"), true)
                .compareTo(new BigDecimal("130.00")), "开票税额");
        // 不开票 → 0。
        assertEquals(0, FeeCalculator.tax(new BigDecimal("1000"), new BigDecimal("13"), false)
                .compareTo(BigDecimal.ZERO), "不开票无税");
    }

    @Test
    void assemble_no_invoice() {
        // 加工费 1075 + 附加费 200，不开票：总额=1275，税额=0。
        FeeCalculator.OrderFee f = FeeCalculator.assemble(
                new BigDecimal("1075"), new BigDecimal("200.00"), false, new BigDecimal("13"));
        assertEquals(0, f.totalAmountTax.compareTo(BigDecimal.ZERO), "不开票总税额0");
        assertEquals(0, f.totalAmount.compareTo(new BigDecimal("1275")), "不含税合计取整");
    }

    @Test
    void assemble_with_invoice() {
        // 加工费 1000 + 附加费 0，开票13%：税额130，总额=1130。
        FeeCalculator.OrderFee f = FeeCalculator.assemble(
                new BigDecimal("1000"), BigDecimal.ZERO, true, new BigDecimal("13"));
        assertEquals(0, f.processAmountTax.compareTo(new BigDecimal("130.00")), "加工费税额");
        assertEquals(0, f.totalAmountTax.compareTo(new BigDecimal("130.00")), "总税额");
        assertEquals(0, f.totalAmount.compareTo(new BigDecimal("1130")), "含税合计取整");
    }
}
