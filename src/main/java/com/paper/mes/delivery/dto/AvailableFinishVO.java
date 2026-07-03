package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 可出库成品视图（该客户已入库、可勾选合并提货的成品，含 source_type=2 直发原纸）。
 */
@Data
public class AvailableFinishVO {

    private String finishUuid;
    private String finishRollNo;
    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private String paperName;
    private Integer gramWeight;
    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    private BigDecimal actualWeight;
    /** 当前剩余可出库重量 kg；为空时旧数据按 actualWeight 兜底。 */
    private BigDecimal remainingWeight;
    /** 1加工产出 2原纸直发 */
    private Integer sourceType;
    /** 1待入库 2已入库 3已出库 4报废 */
    private Integer finishStatus;
    /** 1次结 2月结，来自加工单快照。 */
    private Integer settleType;
    /** 月结对账日，来自加工单快照。 */
    private Integer settleDay;
    private Integer isInvoice;
    /** 该客户是否存在未结清结算单，用于出库风险提示。 */
    private Boolean settlementRisk;
}
