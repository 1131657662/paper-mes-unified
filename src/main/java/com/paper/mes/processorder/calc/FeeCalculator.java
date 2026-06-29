package com.paper.mes.processorder.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 基础计费引擎（P1-5 / V4.1 §2.5/§2.6）。纯函数、无状态、全程 BigDecimal。
 *
 * 计费公式：
 *  - 锯纸费：单工序 = 实际刀数 × 锯纸单价，四舍五入取整
 *  - 复卷费：单工序 = 加工吨位 × 复卷单价，四舍五入取整
 * 逐级取整：每道 step_amount 独立取整，单卷/整单再累加，避免误差累积。
 *
 * 单价/吨位的"非空优先 + 回退"取值由 service 负责（需客户与原纸卷数据）；
 * 本引擎只做"给定单价与数量算金额"与税额拆分。
 */
public final class FeeCalculator {

    public static final int STEP_TYPE_SAW = 1;
    public static final int STEP_TYPE_REWIND = 2;
    public static final BigDecimal TON_DIVISOR = new BigDecimal("1000");

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** 加工费取整到元（scale 0）。 */
    private static final int AMOUNT_SCALE = 0;
    /** 货币金额（税额、附加费、合计中间值）保留 2 位。 */
    private static final int MONEY_SCALE = 2;

    private FeeCalculator() {
    }

    /**
     * 单工序加工费（已四舍五入取整到元）。
     *
     * @param stepType      1锯纸 2复卷
     * @param knifeCount    锯纸实际刀数（锯纸工序用）
     * @param processWeight 复卷加工吨位 吨（复卷工序用）
     * @param unitPrice     本工序单价（锯纸 元/刀 / 复卷 元/吨）
     * @return 取整后的单工序费；单价或数量缺失则为 0
     */
    public static BigDecimal stepAmount(Integer stepType, Integer knifeCount,
                                        BigDecimal processWeight, BigDecimal unitPrice) {
        if (stepType == null || unitPrice == null || unitPrice.signum() == 0) {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE);
        }
        BigDecimal raw;
        if (stepType == STEP_TYPE_SAW) {
            if (knifeCount == null || knifeCount <= 0) {
                return BigDecimal.ZERO.setScale(AMOUNT_SCALE);
            }
            raw = unitPrice.multiply(BigDecimal.valueOf(knifeCount), MC);
        } else if (stepType == STEP_TYPE_REWIND) {
            if (processWeight == null || processWeight.signum() <= 0) {
                return BigDecimal.ZERO.setScale(AMOUNT_SCALE);
            }
            raw = unitPrice.multiply(processWeight, MC);
        } else {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE);
        }
        return raw.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 税额：开票时 = 不含税额 ×(税率%/100)，保留 2 位；不开票时为 0。
     */
    public static BigDecimal tax(BigDecimal noTax, BigDecimal taxRatePct, boolean invoice) {
        if (!invoice || noTax == null || noTax.signum() == 0
                || taxRatePct == null || taxRatePct.signum() == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }
        return noTax.multiply(taxRatePct, MC).divide(HUNDRED, MC)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 由"加工费合计 + 附加费合计 + 是否开票 + 税率"装配整单各金额字段。
     * total_amount 最终四舍五入取整。
     */
    public static OrderFee assemble(BigDecimal totalProcessAmount, BigDecimal totalExtraAmount,
                                    boolean invoice, BigDecimal taxRatePct) {
        BigDecimal processNoTax = nz(totalProcessAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal extraNoTax = nz(totalExtraAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal processTax = tax(processNoTax, taxRatePct, invoice);
        BigDecimal extraTax = tax(extraNoTax, taxRatePct, invoice);
        BigDecimal totalNoTax = processNoTax.add(extraNoTax);
        BigDecimal totalTax = processTax.add(extraTax);
        BigDecimal totalAmount = totalNoTax.add(totalTax).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        return new OrderFee(processNoTax, processTax, extraNoTax, extraTax,
                totalNoTax, totalTax, totalAmount);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 整单计费结果（各金额字段，对齐 biz_process_order 列）。 */
    public static final class OrderFee {
        public final BigDecimal processAmountNoTax;
        public final BigDecimal processAmountTax;
        public final BigDecimal extraAmountNoTax;
        public final BigDecimal extraAmountTax;
        public final BigDecimal totalAmountNoTax;
        public final BigDecimal totalAmountTax;
        public final BigDecimal totalAmount;

        public OrderFee(BigDecimal processAmountNoTax, BigDecimal processAmountTax,
                        BigDecimal extraAmountNoTax, BigDecimal extraAmountTax,
                        BigDecimal totalAmountNoTax, BigDecimal totalAmountTax,
                        BigDecimal totalAmount) {
            this.processAmountNoTax = processAmountNoTax;
            this.processAmountTax = processAmountTax;
            this.extraAmountNoTax = extraAmountNoTax;
            this.extraAmountTax = extraAmountTax;
            this.totalAmountNoTax = totalAmountNoTax;
            this.totalAmountTax = totalAmountTax;
            this.totalAmount = totalAmount;
        }
    }
}
