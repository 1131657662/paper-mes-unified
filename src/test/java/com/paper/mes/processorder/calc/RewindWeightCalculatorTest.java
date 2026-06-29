package com.paper.mes.processorder.calc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RewindWeightCalculator 基准单测，用例取自 P0-0《复卷重量分摊计算规格书》§F。
 * 断言：每件重量 ±0.5kg 容差；整卷 Σ各件 + 总损耗 严格等于 W_actual。
 */
class RewindWeightCalculatorTest {

    private static final BigDecimal TOL = new BigDecimal("0.5");

    private void assertClose(BigDecimal expected, BigDecimal actual) {
        assertTrue(actual.subtract(expected).abs().compareTo(TOL) <= 0,
                "期望≈" + expected + " 实际=" + actual);
    }

    @Test
    void case1_mode2_diameter_no_trim() {
        // W_actual=1000，门幅1200不变；A外径30英寸,B外径28.28英寸,纸芯均3英寸。
        BigDecimal dCore = RewindWeightCalculator.inchToMm(new BigDecimal("3"));
        BigDecimal sA = RewindWeightCalculator.crossSectionArea(
                RewindWeightCalculator.inchToMm(new BigDecimal("30")), dCore);
        BigDecimal sB = RewindWeightCalculator.crossSectionArea(
                RewindWeightCalculator.inchToMm(new BigDecimal("28.28")), dCore);

        List<RewindWeightCalculator.PieceResult> r = RewindWeightCalculator.allocate(
                new BigDecimal("1000.000"),
                List.of(new RewindWeightCalculator.PieceInput(sA, null),
                        new RewindWeightCalculator.PieceInput(sB, null)),
                BigDecimal.ZERO, new BigDecimal("1200"), BigDecimal.ZERO);

        assertClose(new BigDecimal("529.81"), r.get(0).weight);
        assertClose(new BigDecimal("470.19"), r.get(1).weight);
        // 整卷闭合：严格相等。
        assertEquals(0, r.get(0).weight.add(r.get(1).weight)
                .compareTo(new BigDecimal("1000.000")), "整卷闭合");
    }

    @Test
    void case2_mode1_width_with_trim() {
        // W_actual=800，门幅1500；拆3件门幅500/500/480，总修边20mm。
        List<RewindWeightCalculator.PieceResult> r = RewindWeightCalculator.allocate(
                new BigDecimal("800.000"),
                List.of(new RewindWeightCalculator.PieceInput(new BigDecimal("500"), null),
                        new RewindWeightCalculator.PieceInput(new BigDecimal("500"), null),
                        new RewindWeightCalculator.PieceInput(new BigDecimal("480"), null)),
                new BigDecimal("20"), new BigDecimal("1500"), BigDecimal.ZERO);

        assertClose(new BigDecimal("266.714"), r.get(0).weight);
        assertClose(new BigDecimal("266.714"), r.get(1).weight);
        assertClose(new BigDecimal("255.905"), r.get(2).weight);

        // 闭合：Σ各件 = W_actual − 修边总重（末件倒挤吸收修边）。
        // trim_total=(20/1500)*800=10.6667，故 Σ各件应=789.333。
        // 不用 trimWeightShare×3 反推，避免单件四舍五入误差放大。
        BigDecimal sum = r.get(0).weight.add(r.get(1).weight).add(r.get(2).weight);
        assertEquals(0, sum.compareTo(new BigDecimal("789.333")), "整卷闭合(末件吸收修边)");
    }

    @Test
    void case3_measured_priority() {
        // 实称值优先：3件,中间件实称300kg,其余面积均分;W_actual=1000,无修边无损耗。
        List<RewindWeightCalculator.PieceResult> r = RewindWeightCalculator.allocate(
                new BigDecimal("1000.000"),
                List.of(new RewindWeightCalculator.PieceInput(new BigDecimal("100"), null),
                        new RewindWeightCalculator.PieceInput(new BigDecimal("100"), new BigDecimal("300.000")),
                        new RewindWeightCalculator.PieceInput(new BigDecimal("100"), null)),
                BigDecimal.ZERO, new BigDecimal("1000"), BigDecimal.ZERO);

        // 实称件取300;首件按(1000-300)/2=350;末件倒挤=1000-350-300=350。
        assertClose(new BigDecimal("350.000"), r.get(0).weight);
        assertEquals(0, r.get(1).weight.compareTo(new BigDecimal("300.000")), "实称件直接取实称值");
        assertClose(new BigDecimal("350.000"), r.get(2).weight);
        BigDecimal sum = r.get(0).weight.add(r.get(1).weight).add(r.get(2).weight);
        assertEquals(0, sum.compareTo(new BigDecimal("1000.000")), "整卷闭合");
    }

    @Test
    void case4_mode3_diameter_and_width() {
        // 模式3 改直径+改门幅：W=1000，原纸门幅1200，纸芯3英寸。
        // A 外径30英寸/门幅1200(占比1.0)，B 外径30英寸/门幅600(占比0.5)。
        // 两件外径相同，areaBasis 比 = 1:0.5 = 2:1 → A≈666.667，B 倒挤≈333.333。
        BigDecimal dCore = RewindWeightCalculator.inchToMm(new BigDecimal("3"));
        BigDecimal dOut = RewindWeightCalculator.inchToMm(new BigDecimal("30"));
        BigDecimal originalWidth = new BigDecimal("1200");
        BigDecimal basisA = RewindWeightCalculator.areaBasisMode3(dOut, dCore, new BigDecimal("1200"), originalWidth);
        BigDecimal basisB = RewindWeightCalculator.areaBasisMode3(dOut, dCore, new BigDecimal("600"), originalWidth);

        List<RewindWeightCalculator.PieceResult> r = RewindWeightCalculator.allocate(
                new BigDecimal("1000.000"),
                List.of(new RewindWeightCalculator.PieceInput(basisA, null),
                        new RewindWeightCalculator.PieceInput(basisB, null)),
                BigDecimal.ZERO, originalWidth, BigDecimal.ZERO);

        assertClose(new BigDecimal("666.667"), r.get(0).weight);
        assertClose(new BigDecimal("333.333"), r.get(1).weight);
        assertEquals(0, r.get(0).weight.add(r.get(1).weight)
                .compareTo(new BigDecimal("1000.000")), "整卷闭合");
    }

    @Test
    void case5_mode4_layered() {
        // 模式4 内外层分层：W=1000，纸芯3英寸。
        // A 单层(外径30英寸)；B 双层(外径24英寸 + 外径18英寸)汇总面积。
        // areaBasis 由 layeredArea 构造，断言按面积比分配且整卷闭合。
        BigDecimal dCore = RewindWeightCalculator.inchToMm(new BigDecimal("3"));
        BigDecimal basisA = RewindWeightCalculator.layeredArea(List.of(
                new RewindWeightCalculator.Layer(RewindWeightCalculator.inchToMm(new BigDecimal("30")), dCore)));
        BigDecimal basisB = RewindWeightCalculator.layeredArea(List.of(
                new RewindWeightCalculator.Layer(RewindWeightCalculator.inchToMm(new BigDecimal("24")), dCore),
                new RewindWeightCalculator.Layer(RewindWeightCalculator.inchToMm(new BigDecimal("18")), dCore)));

        BigDecimal wActual = new BigDecimal("1000.000");
        List<RewindWeightCalculator.PieceResult> r = RewindWeightCalculator.allocate(
                wActual,
                List.of(new RewindWeightCalculator.PieceInput(basisA, null),
                        new RewindWeightCalculator.PieceInput(basisB, null)),
                BigDecimal.ZERO, new BigDecimal("1200"), BigDecimal.ZERO);

        // 期望 = W × 该件面积 / 总面积（与引擎同源，验证 layeredArea 汇总正确）。
        BigDecimal total = basisA.add(basisB);
        BigDecimal expA = wActual.multiply(basisA).divide(total, 3, java.math.RoundingMode.HALF_UP);
        BigDecimal expB = wActual.multiply(basisB).divide(total, 3, java.math.RoundingMode.HALF_UP);
        assertClose(expA, r.get(0).weight);
        assertClose(expB, r.get(1).weight);
        assertEquals(0, r.get(0).weight.add(r.get(1).weight)
                .compareTo(wActual), "整卷闭合");
    }

    @Test
    void case6_mode5_merge_ratio() {
        // 模式5 多母卷合并复卷：合计 W=1000，按比例 0.5/0.3/0.2 直接作 areaBasis。
        // 占比即 areaBasis 比，分摊结果 = W×ratio_i（末件倒挤）。
        List<RewindWeightCalculator.PieceResult> r = RewindWeightCalculator.allocate(
                new BigDecimal("1000.000"),
                List.of(new RewindWeightCalculator.PieceInput(new BigDecimal("0.5"), null),
                        new RewindWeightCalculator.PieceInput(new BigDecimal("0.3"), null),
                        new RewindWeightCalculator.PieceInput(new BigDecimal("0.2"), null)),
                BigDecimal.ZERO, new BigDecimal("1200"), BigDecimal.ZERO);

        assertClose(new BigDecimal("500.000"), r.get(0).weight);
        assertClose(new BigDecimal("300.000"), r.get(1).weight);
        assertClose(new BigDecimal("200.000"), r.get(2).weight);
        BigDecimal sum = r.get(0).weight.add(r.get(1).weight).add(r.get(2).weight);
        assertEquals(0, sum.compareTo(new BigDecimal("1000.000")), "整卷闭合");
    }
}
