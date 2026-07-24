package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class FinishCustomerRevisionItemVO {
    private String finishUuid;
    private String finishRollNo;
    private String physicalPaperName;
    private Integer physicalGramWeight;
    private Integer physicalFinishWidth;
    private BigDecimal physicalWeightSnapshot;
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
    private String remark;
}
