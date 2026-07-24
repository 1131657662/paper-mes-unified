package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryCustomerSpecVO {

    private String deliveryDetailUuid;
    private Integer detailVersion;
    private String finishUuid;
    private String finishRollNo;
    private String orderUuid;
    private String orderNo;
    private String physicalPaperName;
    private Integer physicalGramWeight;
    private Integer physicalFinishWidth;
    private BigDecimal physicalDeliveryWeight;
    private String previousCustomerPaperName;
    private Integer previousCustomerGramWeight;
    private Integer previousCustomerFinishWidth;
    private BigDecimal previousCustomerDisplayWeight;
    private String customerPaperName;
    private Integer customerGramWeight;
    private Integer customerFinishWidth;
    private BigDecimal customerDisplayWeight;
    private String customerRemark;
    private String calculationMode;
    private String valueSource;
    private boolean specificationChanged;
    private boolean weightChanged;
    private boolean valid;
    private String error;
}
