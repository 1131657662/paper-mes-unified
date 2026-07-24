package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinishCustomerRevisionSummaryVO {

    private String uuid;
    private String orderUuid;
    private Integer revisionNo;
    private String sourceStage;
    private String reason;
    private Integer itemCount;
    private BigDecimal customerTotalWeight;
    private String operator;
    private LocalDateTime createdAt;
}
