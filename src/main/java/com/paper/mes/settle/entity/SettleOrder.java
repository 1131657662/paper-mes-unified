package com.paper.mes.settle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算单主表 biz_settle_order。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settle_order")
public class SettleOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String settleNo;
    private String customerUuid;
    private String customerName;
    /** 1按单 2按月批量 */
    private Integer settleType;
    private LocalDate settleDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    private BigDecimal extraAmount;
    private BigDecimal amountNoTax;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unreceivedAmount;
    /** 1开票 2不开票 */
    private Integer isInvoice;
    /** 1待结算 2部分收款 3全部结清 */
    private Integer settleStatus;
    private String remark;
}
