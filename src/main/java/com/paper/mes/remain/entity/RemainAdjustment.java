package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_remain_adjustment")
public class RemainAdjustment extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String adjustmentNo;
    private String requestId;
    private String requestHash;
    private String registrationUuid;
    private String sourceSettleUuid;
    private String targetSettleUuid;
    private String customerUuid;
    private String targetType;
    private String status;
    private BigDecimal amount;
    private BigDecimal weight;
    private String reason;
}
