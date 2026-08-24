package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RemainRegistrationVO {

    private String uuid;
    private String registrationNo;
    private String requestId;
    private String orderUuid;
    private String customerUuid;
    private LocalDateTime registrationDate;
    private String confirmationName;
    private String confirmationChannel;
    private LocalDateTime confirmationAt;
    private String confirmationEvidence;
    private String status;
    private String priceStatus;
    private Integer priceVersion;
    private String pricingBasis;
    private LocalDateTime priceConfirmedAt;
    private String priceConfirmedBy;
    private BigDecimal totalTransferredWeight;
    private BigDecimal totalRolledBackWeight;
    private BigDecimal totalProcessedWeight;
    private BigDecimal totalAmount;
    private List<RemainRegistrationLineVO> lines;
}
