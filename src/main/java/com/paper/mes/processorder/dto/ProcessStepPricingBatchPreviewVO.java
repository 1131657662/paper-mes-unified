package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProcessStepPricingBatchPreviewVO {
    private String orderUuid;
    private String orderNo;
    private Integer orderVersion;
    private Integer stepCount;
    private BigDecimal standardAmount;
    private BigDecimal currentAmount;
    private BigDecimal finalAmount;
    private BigDecimal adjustmentAmount;
    private List<Row> rows;

    @Data
    public static class Row {
        private String stepUuid;
        private String originalUuid;
        private Integer stepType;
        private String stepName;
        private Integer billingMode;
        private String billingBasis;
        private BigDecimal quantity;
        private BigDecimal standardUnitPrice;
        private BigDecimal currentUnitPrice;
        private BigDecimal finalUnitPrice;
        private BigDecimal standardAmount;
        private BigDecimal currentAmount;
        private BigDecimal finalAmount;
        private BigDecimal adjustmentAmount;
    }
}
