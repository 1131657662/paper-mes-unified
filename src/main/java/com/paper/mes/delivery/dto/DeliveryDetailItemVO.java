package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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
    private List<OriginalSourceItem> originalItems;
    private List<ProcessStepItem> processStepItems;
    private String remark;
    private String finishRemark;
    private String actualRemark;

    @Data
    public static class OriginalSourceItem {
        private String uuid;
        private Integer rowSort;
        private String extraNo;
        private String rollNo;
        private String paperName;
        private Integer gramWeight;
        private Integer actualGramWeight;
        private Integer originalWidth;
        private Integer actualWidth;
        private BigDecimal actualWeight;
        private BigDecimal totalWeight;
        private Integer processMode;
        private Integer mainStepType;
        private String machineUuid;
        private String machineName;
        private String operator;
        private String remark;
    }

    @Data
    public static class ProcessStepItem {
        private String uuid;
        private String originalUuid;
        private Integer stepSort;
        private Integer stepType;
        private String stepName;
        private Integer isMain;
        private Integer knifeCount;
        private BigDecimal processWeight;
        private BigDecimal unitPrice;
        private BigDecimal stepAmount;
        private BigDecimal lossWeight;
        private String operator;
        private String remark;
    }
}
