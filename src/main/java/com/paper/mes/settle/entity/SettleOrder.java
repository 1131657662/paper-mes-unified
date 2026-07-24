package com.paper.mes.settle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String requestId;
    private String quoteVersion;
    private String quoteHash;
    /** 1按单 2按月批量 3勾选合并 */
    private Integer settleType;
    private LocalDate settleDate;
    private LocalDate dueDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    private BigDecimal serviceAmount;
    private BigDecimal extraAmount;
    private BigDecimal amountNoTax;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal cashReceivedAmount;
    private BigDecimal scrapOffsetAmount;
    private BigDecimal discountAmount;
    private BigDecimal unreceivedAmount;
    private Integer reminderCount;
    private LocalDateTime lastReminderTime;
    private String lastReminderBy;
    private Integer lastReminderResult;
    private LocalDate nextFollowUpDate;
    /** 1开票 2不开票 */
    private Integer isInvoice;
    /** 1待收款 2部分收款 3全部结清 4已作废 */
    private Integer settleStatus;
    private String voidReason;
    private String voidBy;
    private LocalDateTime voidTime;
    private String snapBill;
    private LocalDateTime snapBillTime;
    private String remark;
}
