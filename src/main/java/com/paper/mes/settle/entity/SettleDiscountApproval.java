package com.paper.mes.settle.entity;

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
@TableName("biz_settle_discount_approval")
public class SettleDiscountApproval extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String settleUuid;
    private String requestId;
    private BigDecimal discountAmount;
    private String reason;
    /** 1待审批 2已批准 3已使用 4已拒绝 */
    private Integer approvalStatus;
    private String requestBy;
    private String requestByName;
    private LocalDateTime requestTime;
    private String approveBy;
    private String approveByName;
    private LocalDateTime approveTime;
    private String usedReceiveUuid;
}
