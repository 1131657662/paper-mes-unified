package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class DeliveryCustomerRevisionItemVO {
    private String deliveryDetailUuid;
    private String finishUuid;
    private String finishRollNo;
    private String physicalPaperName;
    private Integer physicalGramWeight;
    private Integer physicalFinishWidth;
    private BigDecimal physicalDeliveryWeight;
    private String customerPaperName;
    private Integer customerGramWeight;
    private Integer customerFinishWidth;
    private BigDecimal customerDisplayWeight;
    private String calculationMode;
    private BigDecimal weightOperand;
    private String formulaExpression;
    private Map<String, BigDecimal> formulaVariables;
    private Integer roundingScale;
    private String roundingMode;
    private String zeroPolicy;
    private String customerRemark;
}
