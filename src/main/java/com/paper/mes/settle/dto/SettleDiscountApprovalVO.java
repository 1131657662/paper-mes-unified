package com.paper.mes.settle.dto;

import com.paper.mes.settle.entity.SettleDiscountApproval;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettleDiscountApprovalVO {
    private String uuid;
    private BigDecimal discountAmount;
    private String reason;
    private Integer approvalStatus;
    private String requestByName;
    private LocalDateTime requestTime;
    private String approveByName;
    private LocalDateTime approveTime;
    private String usedReceiveUuid;

    public static SettleDiscountApprovalVO from(SettleDiscountApproval item) {
        SettleDiscountApprovalVO vo = new SettleDiscountApprovalVO();
        vo.setUuid(item.getUuid());
        vo.setDiscountAmount(item.getDiscountAmount());
        vo.setReason(item.getReason());
        vo.setApprovalStatus(item.getApprovalStatus());
        vo.setRequestByName(item.getRequestByName());
        vo.setRequestTime(item.getRequestTime());
        vo.setApproveByName(item.getApproveByName());
        vo.setApproveTime(item.getApproveTime());
        vo.setUsedReceiveUuid(item.getUsedReceiveUuid());
        return vo;
    }
}
