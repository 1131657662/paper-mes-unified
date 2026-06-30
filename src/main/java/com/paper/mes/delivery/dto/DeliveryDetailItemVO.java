package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 出库单明细展示项：以出库明细为主，批量补齐成品卷与加工单追溯字段。
 */
@Data
public class DeliveryDetailItemVO {

    private String uuid;
    private String deliveryUuid;
    private String finishUuid;
    private String orderUuid;
    private String orderNo;
    private String finishRollNo;
    private String paperName;
    private Integer gramWeight;
    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    private BigDecimal actualWeight;
    private BigDecimal outWeight;
    private Integer sourceType;
    private Integer finishStatus;
    private String originalRollNos;
    private String originalSummary;
    private String processModeText;
    private String processSummary;
    private String remark;
    private String finishRemark;
    private String actualRemark;
}
