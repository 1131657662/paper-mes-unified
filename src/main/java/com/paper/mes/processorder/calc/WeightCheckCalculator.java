package com.paper.mes.processorder.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 三级重量闭合校验引擎（V4.1 §5.7）。纯函数、无状态。
 *
 * 闭合公式（单卷维度）：
 *   理论合计 = Σ成品实际重量 + Σ工序损耗 + Σ报废重量 + Σ修边重量
 *   偏差     = |W_actual − 理论合计|
 *   偏差率   = 偏差 / W_actual
 *
 * 三级阈值：
 *   PASS   |偏差率| ≤ 2%      正常闭合
 *   WARN   2% < |率| ≤ 5%     强警告，回录需填原因
 *   BLOCK  |偏差率| > 5%       拦截，需管理员授权放行并写操作日志
 */
public final class WeightCheckCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal WARN_THRESHOLD = new BigDecimal("2");
    private static final BigDecimal BLOCK_THRESHOLD = new BigDecimal("5");
    private static final int RATIO_SCALE = 2;
    private static final int WEIGHT_SCALE = 3;

    private WeightCheckCalculator() {
    }

    public enum Level {
        /** ≤2% 正常闭合 */
        PASS,
        /** 2~5% 强警告，需填原因 */
        WARN,
        /** >5% 拦截，需授权放行 */
        BLOCK
    }

    /**
     * 计算单卷三级闭合校验结果。
     *
     * @param wActual       原纸复称实际重量 kg（闭合唯一基准，必须 &gt; 0）
     * @param finishSum     该卷下全部成品实际重量合计 kg
     * @param lossSum       该卷全工序损耗合计 kg
     * @param scrapSum      该卷报废重量合计 kg
     * @param trimSum       该卷修边重量合计 kg
     */
    public static CheckResult check(BigDecimal wActual, BigDecimal finishSum,
                                    BigDecimal lossSum, BigDecimal scrapSum, BigDecimal trimSum) {
        BigDecimal actual = nz(wActual);
        BigDecimal theoretical = nz(finishSum).add(nz(lossSum)).add(nz(scrapSum)).add(nz(trimSum));
        BigDecimal diff = actual.subtract(theoretical).abs();

        BigDecimal ratioPct;
        if (actual.signum() == 0) {
            // 无基准重量无法闭合，按拦截处理（理论合计非零时）。
            ratioPct = theoretical.signum() == 0 ? BigDecimal.ZERO
                    : BLOCK_THRESHOLD.add(BigDecimal.ONE);
        } else {
            ratioPct = diff.divide(actual, MC).multiply(HUNDRED, MC);
        }

        Level level;
        if (ratioPct.compareTo(WARN_THRESHOLD) <= 0) {
            level = Level.PASS;
        } else if (ratioPct.compareTo(BLOCK_THRESHOLD) <= 0) {
            level = Level.WARN;
        } else {
            level = Level.BLOCK;
        }

        return new CheckResult(
                level,
                actual.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP),
                theoretical.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP),
                actual.subtract(theoretical).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP),
                ratioPct.setScale(RATIO_SCALE, RoundingMode.HALF_UP));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 单卷闭合校验结果。diffWeight 保留符号（正=实际偏大，负=理论偏大）。 */
    public static final class CheckResult {
        public final Level level;
        public final BigDecimal actualWeight;
        public final BigDecimal theoreticalWeight;
        public final BigDecimal diffWeight;
        public final BigDecimal diffRatioPct;

        public CheckResult(Level level, BigDecimal actualWeight, BigDecimal theoreticalWeight,
                           BigDecimal diffWeight, BigDecimal diffRatioPct) {
            this.level = level;
            this.actualWeight = actualWeight;
            this.theoreticalWeight = theoreticalWeight;
            this.diffWeight = diffWeight;
            this.diffRatioPct = diffRatioPct;
        }

        public boolean isPass() {
            return level == Level.PASS;
        }

        public boolean isBlock() {
            return level == Level.BLOCK;
        }
    }
}
