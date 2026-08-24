package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RemainRefundVO {
    private String uuid;
    private String refundNo;
    private String adjustmentUuid;
    private String customerUuid;
    private BigDecimal amount;
    private BigDecimal weight;
    private String status;
    private String paymentReference;
    private String reason;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
}
