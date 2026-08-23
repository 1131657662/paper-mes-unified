package com.paper.mes.processorder.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * 复卷重量分摊计算引擎（P0-0《复卷重量分摊计算规格书》/ V4.1 §2.6.1）。
 *
 * 纯函数、无状态。预估重量以整数 kg 分配并闭合；实际回录重量保留三位小数。
 * 核心规则：
 *  - 实称值优先（§G）：已实称件直接取实称值，面积分摊仅作用于剩余未实称件，
 *    且先从来源重量扣除已实称件重量与总损耗后再分摊。
 *  - 预估整数最大余数分配（§E）：成品和余料按整数 kg 分配并严格闭合。
 */
public final class RewindWeightCalculator {

    private static final BigDecimal INCH_TO_MM = new BigDecimal("25.4");
    private static final BigDecimal PI = new BigDecimal(Math.PI);
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private RewindWeightCalculator() {
    }

    /** 英寸 → 毫米。 */
    public static BigDecimal inchToMm(BigDecimal inch) {
        return inch.multiply(INCH_TO_MM, MC);
    }

    /** 历史卷径小值以英寸保存，现行数据直接以毫米保存。 */
    public static BigDecimal storedDiameterToMm(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.signum() > 0 && value.compareTo(new BigDecimal("100")) < 0
                ? inchToMm(value) : value;
    }

    /** 纸芯历史数据通常以 3/6 英寸保存，76/152 则按毫米保存。 */
    public static BigDecimal storedCoreDiameterToMm(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.signum() > 0 && value.compareTo(new BigDecimal("10")) < 0
                ? inchToMm(value) : value;
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
     * @param wActual    来源有效总重 kg（预估闭合基准）
     * @param pieces     各件分摊基准：面积/门幅权重 areaBasis + 可选实称重量 actualWeight
     * @param trimTotalWidth 总修边宽度 mm（≤1mm 视为豁免，按 0 处理见 §2.8，调用方负责豁免清零）
     * @param originalWidth  原纸门幅 mm（修边占比分母）
     * @param totalLoss  总损耗重量 kg（预估场景按整数处理）
     * @return 每件分摊结果（顺序与入参一致）
     *
     * <p>模式5（多母卷合并复卷）无需新增方法：将各母卷比例 ratio_i 直接作 areaBasis 传入、
     * wActual 传各母卷合计重，Σratio=1 时占比即 ratio_i。
     */
    public static List<PieceResult> allocate(BigDecimal wActual, List<PieceInput> pieces,
                                             BigDecimal trimTotalWidth, BigDecimal originalWidth,
                                             BigDecimal totalLoss) {
        return RewindEstimateWeightAllocator.allocate(
                wActual, pieces, trimTotalWidth, originalWidth, totalLoss);
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
