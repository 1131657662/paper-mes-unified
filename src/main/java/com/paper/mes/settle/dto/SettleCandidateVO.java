package com.paper.mes.settle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算工作台候选加工单视图。
 */
@Data
public class SettleCandidateVO {

    private String orderUuid;
    private String orderNo;
    private String customerUuid;
    private String customerName;
    private LocalDate orderDate;
    /** 回录完成日期；历史数据缺失回录时间时回退到制单日期。 */
    private LocalDate accountingDate;
    /** 1次结 2月结，来自加工单快照。 */
    private Integer settleType;
    private Integer settleDay;
    private Integer isInvoice;
    private Integer originalRollCount;
    private BigDecimal originalRollWeight;
    private Integer finishRollCount;
    private BigDecimal finishRollWeight;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    /** 璁㈠崟鏍囧噯鍔犲伐璐癸紝鐢ㄤ簬鍦ㄧ敓鎴愮粨绠楀墠灞曠ず浼樻儬瀹℃牳鍩虹銆? */
    private BigDecimal standardProcessAmount;
    /** 鏈€缁堣浠峰噺鏍囧噯璁′环锛岃礋鏁颁负浼樻儬銆? */
    private BigDecimal pricingAdjustmentAmount;
    private BigDecimal extraAmount;
    private BigDecimal totalAmount;
}
