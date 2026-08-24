package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RemainAdjustmentVO {
    private String uuid;
    private String adjustmentNo;
    private String registrationUuid;
    private String sourceSettleUuid;
    private String targetSettleUuid;
    private String customerUuid;
    private String targetType;
    private String status;
    private BigDecimal amount;
    private BigDecimal weight;
    private String reason;
    private List<RemainAdjustmentLineVO> lines;
}
