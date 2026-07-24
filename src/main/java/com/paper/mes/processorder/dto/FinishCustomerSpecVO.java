package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinishCustomerSpecVO {

    private String finishUuid;
    private String finishRollNo;
    private Integer rowSort;
    private Integer finishVersion;
    private String physicalPaperName;
    private Integer physicalGramWeight;
    private Integer physicalFinishWidth;
    private BigDecimal physicalWeight;
    private String previousCustomerPaperName;
    private Integer previousCustomerGramWeight;
    private Integer previousCustomerFinishWidth;
    private BigDecimal previousCustomerDisplayWeight;
    private String customerPaperName;
    private Integer customerGramWeight;
    private Integer customerFinishWidth;
    private BigDecimal customerDisplayWeight;
    private String calculationMode;
    private boolean specificationChanged;
    private boolean weightChanged;
    private boolean valid;
    private String error;
}
