package com.paper.mes.settle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款流水 biz_receive_record。一张结算单可多次分批收款。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_receive_record")
public class ReceiveRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String settleUuid;
    private LocalDateTime receiveDate;
    private BigDecimal receiveAmount;
    /** 1现金 2转账 3微信 4支付宝 */
    private Integer payMethod;
    private String payNo;
    private String operator;
    /** 1有效 2已撤销 */
    private Integer recordStatus;
    private LocalDateTime cancelTime;
    private String cancelBy;
    private String cancelReason;
    private String remark;
}
