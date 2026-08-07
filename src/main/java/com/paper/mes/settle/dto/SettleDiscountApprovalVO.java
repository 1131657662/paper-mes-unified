package com.paper.mes.settle.dto;

import com.paper.mes.settle.entity.SettleDiscountApproval;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettleDiscountApprovalVO {
    private String uuid;
    private String settleUuid;
    private String settleNo;
    private String customerName;
    private BigDecimal cashAmount;
    private BigDecimal scrapOffsetAmount;
    private BigDecimal discountAmount;
    private BigDecimal unreceivedSnapshot;
    private BigDecimal discountPercent;
    private String requiredLevel;
    private String reason;
    private Integer approvalStatus;
    private String requestBy;
    private String requestByName;
    private LocalDateTime requestTime;
    private String approveByName;
    private LocalDateTime approveTime;
    private String decisionReason;
    private String cancelByName;
    private LocalDateTime cancelTime;
    private String policyVersion;
    private String usedReceiveUuid;

    public static SettleDiscountApprovalVO from(SettleDiscountApproval item) {
        SettleDiscountApprovalVO vo = new SettleDiscountApprovalVO();
        vo.setUuid(item.getUuid());
        vo.setSettleUuid(item.getSettleUuid());
        vo.setCashAmount(item.getCashAmount());
        vo.setScrapOffsetAmount(item.getScrapOffsetAmount());
        vo.setDiscountAmount(item.getDiscountAmount());
        vo.setUnreceivedSnapshot(item.getUnreceivedSnapshot());
        vo.setDiscountPercent(item.getDiscountPercent());
        vo.setRequiredLevel(item.getRequiredLevel());
        vo.setReason(item.getReason());
        vo.setApprovalStatus(item.getApprovalStatus());
        vo.setRequestBy(item.getRequestBy());
        vo.setRequestByName(item.getRequestByName());
        vo.setRequestTime(item.getRequestTime());
        vo.setApproveByName(item.getApproveByName());
        vo.setApproveTime(item.getApproveTime());
        vo.setDecisionReason(item.getDecisionReason());
        vo.setCancelByName(item.getCancelByName());
        vo.setCancelTime(item.getCancelTime());
        vo.setPolicyVersion(item.getPolicyVersion());
        vo.setUsedReceiveUuid(item.getUsedReceiveUuid());
        return vo;
    }

    public static SettleDiscountApprovalVO from(SettleDiscountApproval item,
                                                 com.paper.mes.settle.entity.SettleOrder settle) {
        SettleDiscountApprovalVO vo = from(item);
        if (settle != null) {
            vo.setSettleNo(settle.getSettleNo());
            vo.setCustomerName(settle.getCustomerName());
        }
        return vo;
    }
}
