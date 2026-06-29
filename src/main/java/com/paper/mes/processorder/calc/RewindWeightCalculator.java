package com.paper.mes.processorder.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 复卷重量分摊计算引擎（P0-0《复卷重量分摊计算规格书》/ V4.1 §2.6.1）。
 *
 * 纯函数、无状态。全程 BigDecimal，最终重量四舍五入到 3 位小数（对齐 decimal(10,3)）。
 * 核心规则：
 *  - 实称值优先（§G）：已实称件直接取实称值，面积分摊仅作用于剩余未实称件，
 *    且先从 W_actual 扣除已实称件重量与总损耗后再分摊。
 *  - 末件尾差倒挤（§E）：最后一件由整卷余量倒挤，保证 Σ各件 + 总损耗 严格等于 W_actual。
 */
public final class RewindWeightCalculator {

    private static final BigDecimal INCH_TO_MM = new BigDecimal("25.4");
    private static final BigDecimal PI = new BigDecimal(Math.PI);
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int WEIGHT_SCALE = 3;

    private RewindWeightCalculator() {
    }

    /** 英寸 → 毫米。 */
    public static BigDecimal inchToMm(BigDecimal inch) {
        return inch.multiply(INCH_TO_MM, MC);
    }

    /** 纸卷横截面积 S = π × ((D_out/2)² − (D_core/2)²)，单位 mm²。入参为 mm。 */
    public static BigDecimal crossSectionArea(BigDecimal dOutMm, BigDecimal dCoreMm) {
        BigDecimal rOut = dOutMm.divide(TWO, MC);
        BigDecimal rCore = dCoreMm.divide(TWO, MC);
        BigDecimal diff = rOut.multiply(rOut, MC).subtract(rCore.multiply(rCore, MC));
        return PI.multiply(diff, MC);
    }

    /**
     * 模式3（改直径 + 改门幅）areaBasis = 横截面积 × 门幅占比。
     * 即在模式2截面积基础上，按该件门幅相对原纸门幅折算，门幅与直径双变量同时体现。
     *
     * @param dOutMm        该件外径 mm
     * @param dCoreMm       该件纸芯内径 mm
     * @param width         该件门幅 mm
     * @param originalWidth 原纸门幅 mm
     */
    public static BigDecimal areaBasisMode3(BigDecimal dOutMm, BigDecimal dCoreMm,
                                            BigDecimal width, BigDecimal originalWidth) {
        BigDecimal area = crossSectionArea(dOutMm, dCoreMm);
        if (originalWidth == null || originalWidth.signum() == 0) {
            return area;
        }
        return area.multiply(width.divide(originalWidth, MC), MC);
    }

    /**
     * 模式4（内外层分层）areaBasis = 各层横截面积之和。
     * 同一成品件由多层组成时，逐层按 (层外径,层内径) 求截面积后汇总作分摊基准。
     */
    public static BigDecimal layeredArea(List<Layer> layers) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Layer l : layers) {
            sum = sum.add(crossSectionArea(l.dOutMm, l.dCoreMm), MC);
        }
        return sum;
    }

    /**
     * 单卷成品重量分摊。
     *
     * @param wActual    原纸实际总重 kg（闭合唯一基准）
     * @param pieces     各件分摊基准：面积/门幅权重 areaBasis + 可选实称重量 actualWeight
     * @param trimTotalWidth 总修边宽度 mm（≤1mm 视为豁免，按 0 处理见 §2.8，调用方负责豁免清零）
     * @param originalWidth  原纸门幅 mm（修边占比分母）
     * @param totalLoss  总损耗重量 kg（Σ工序损耗，参与末件倒挤）
     * @return 每件分摊结果（顺序与入参一致），末件为倒挤值
     *
     * <p>模式5（多母卷合并复卷）无需新增方法：将各母卷比例 ratio_i 直接作 areaBasis 传入、
     * wActual 传各母卷合计重，Σratio=1 时占比即 ratio_i，分摊结果即 W_actual×ratio_i（末件倒挤闭合）。
     */
    public static List<PieceResult> allocate(BigDecimal wActual, List<PieceInput> pieces,
                                             BigDecimal trimTotalWidth, BigDecimal originalWidth,
                                             BigDecimal totalLoss) {
        int n = pieces.size();
        if (n == 0) {
            return List.of();
        }
        BigDecimal loss = totalLoss == null ? BigDecimal.ZERO : totalLoss;

        // 修边总重与单件分摊：trim_total = (总修边宽度 / 原纸门幅) × W_actual；share = trim_total / N。
        BigDecimal trimTotal = BigDecimal.ZERO;
        if (trimTotalWidth != null && trimTotalWidth.signum() > 0
                && originalWidth != null && originalWidth.signum() > 0) {
            trimTotal = trimTotalWidth.divide(originalWidth, MC).multiply(wActual, MC);
        }
        BigDecimal trimShare = trimTotal.divide(BigDecimal.valueOf(n), MC);

        // 实称值优先：先扣除已实称件重量，剩余在未实称件间按面积分摊。
        BigDecimal measuredSum = BigDecimal.ZERO;
        BigDecimal unmeasuredAreaTotal = BigDecimal.ZERO;
        for (PieceInput p : pieces) {
            if (p.actualWeight != null) {
                measuredSum = measuredSum.add(p.actualWeight);
            } else {
                unmeasuredAreaTotal = unmeasuredAreaTotal.add(p.areaBasis);
            }
        }
        // 未实称件可分配的总量 = W_actual − 已实称合计 − 总损耗 − 修边总重。
        // 修边重量是整卷损耗，从分配池整体扣除；非末件再各自减 trimShare 体现到件重。
        BigDecimal distributable = wActual.subtract(measuredSum).subtract(loss).subtract(trimTotal);

        List<PieceResult> results = new ArrayList<>(n);
        BigDecimal allocatedExceptLast = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            PieceInput p = pieces.get(i);
            boolean isLast = (i == n - 1);
            BigDecimal weight;
            if (isLast) {
                // 末件倒挤：W_actual − Σ前(N−1)件 − 总损耗 − 修边总重，吸收全部尾差。
                weight = wActual.subtract(allocatedExceptLast).subtract(loss).subtract(trimTotal)
                        .setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
            } else if (p.actualWeight != null) {
                weight = p.actualWeight.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
                allocatedExceptLast = allocatedExceptLast.add(weight);
            } else {
                BigDecimal raw = unmeasuredAreaTotal.signum() == 0
                        ? BigDecimal.ZERO
                        : distributable.multiply(p.areaBasis, MC).divide(unmeasuredAreaTotal, MC);
                weight = raw.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
                allocatedExceptLast = allocatedExceptLast.add(weight);
            }
            results.add(new PieceResult(weight, trimShare.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP)));
        }
        return results;
    }

    /** 模式4 分层入参：单层的外径与纸芯内径 mm。 */
    public static final class Layer {
        public final BigDecimal dOutMm;
        public final BigDecimal dCoreMm;

        public Layer(BigDecimal dOutMm, BigDecimal dCoreMm) {
            this.dOutMm = dOutMm;
            this.dCoreMm = dCoreMm;
        }
    }

    /** 单件分摊入参。areaBasis：模式1传门幅，模式2/3传横截面积（或面积×门幅占比）。 */
    public static final class PieceInput {
        public final BigDecimal areaBasis;
        public final BigDecimal actualWeight; // 实称重量，null 表示未实称走分摊

        public PieceInput(BigDecimal areaBasis, BigDecimal actualWeight) {
            this.areaBasis = areaBasis == null ? BigDecimal.ZERO : areaBasis;
            this.actualWeight = actualWeight;
        }
    }

    /** 单件分摊结果。 */
    public static final class PieceResult {
        public final BigDecimal weight;
        public final BigDecimal trimWeightShare;

        public PieceResult(BigDecimal weight, BigDecimal trimWeightShare) {
            this.weight = weight;
            this.trimWeightShare = trimWeightShare;
        }
    }
}
