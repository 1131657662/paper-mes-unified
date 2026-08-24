package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_remain_refund")
public class RemainRefund extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String refundNo;
    private String adjustmentUuid;
    private String customerUuid;
    private BigDecimal amount;
    private BigDecimal weight;
    private String status;
    private String requestId;
    private String requestHash;
    private String approveRequestId;
    private String payRequestId;
    private String cancelRequestId;
    private String paymentReference;
    private String reason;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String paidBy;
    private LocalDateTime paidAt;
}
