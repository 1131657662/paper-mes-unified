package com.paper.mes.processorder.calc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WeightCheckCalculator 三级闭合阈值单测（V4.1 §5.7）。
 */
class WeightCheckCalculatorTest {

    private static final BigDecimal Z = BigDecimal.ZERO;

    @Test
    void pass_within_2pct() {
        // W=1000，理论合计=990，偏差率=1% → PASS。
        WeightCheckCalculator.CheckResult r = WeightCheckCalculator.check(
                new BigDecimal("1000"), new BigDecimal("980"),
                new BigDecimal("10"), Z, Z);
        assertEquals(WeightCheckCalculator.Level.PASS, r.level);
        assertEquals(0, r.diffRatioPct.compareTo(new BigDecimal("1.00")));
    }

    @Test
    void boundary_exactly_2pct_is_pass() {
        // 偏差率正好=2% → PASS（含等号）。
        WeightCheckCalculator.CheckResult r = WeightCheckCalculator.check(
                new BigDecimal("1000"), new BigDecimal("980"), Z, Z, Z);
        assertEquals(WeightCheckCalculator.Level.PASS, r.level);
    }

    @Test
    void warn_between_2_and_5pct() {
        // W=1000，理论=960，偏差率=4% → WARN。
        WeightCheckCalculator.CheckResult r = WeightCheckCalculator.check(
                new BigDecimal("1000"), new BigDecimal("960"), Z, Z, Z);
        assertEquals(WeightCheckCalculator.Level.WARN, r.level);
        assertEquals(0, r.diffRatioPct.compareTo(new BigDecimal("4.00")));
    }

    @Test
    void boundary_exactly_5pct_is_warn() {
        // 偏差率正好=5% → WARN（含等号），>5 才拦截。
        WeightCheckCalculator.CheckResult r = WeightCheckCalculator.check(
                new BigDecimal("1000"), new BigDecimal("950"), Z, Z, Z);
        assertEquals(WeightCheckCalculator.Level.WARN, r.level);
    }

    @Test
    void block_over_5pct() {
        // W=1000，理论=900，偏差率=10% → BLOCK。
        WeightCheckCalculator.CheckResult r = WeightCheckCalculator.check(
                new BigDecimal("1000"), new BigDecimal("900"), Z, Z, Z);
        assertEquals(WeightCheckCalculator.Level.BLOCK, r.level);
        assertEquals(0, r.diffRatioPct.compareTo(new BigDecimal("10.00")));
    }

    @Test
    void full_closure_components() {
        // 成品900 + 损耗50 + 报废30 + 修边20 = 1000 = W，偏差0 → PASS。
        WeightCheckCalculator.CheckResult r = WeightCheckCalculator.check(
                new BigDecimal("1000"), new BigDecimal("900"),
                new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"));
        assertEquals(WeightCheckCalculator.Level.PASS, r.level);
        assertEquals(0, r.diffWeight.compareTo(new BigDecimal("0.000")));
        assertEquals(0, r.theoreticalWeight.compareTo(new BigDecimal("1000.000")));
    }
}
