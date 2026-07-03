package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加工单主表 biz_process_order。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_order")
public class ProcessOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderNo;
    private String customerUuid;
    /** 快照冗余客户名 */
    private String customerName;
    private LocalDate orderDate;
    private LocalDate expectFinishDate;
    /** 1普通 2加急 3特急 */
    private Integer priority;
    private String labelBrand;
    private String warehouseUuid;
    private String teamGroup;

    /** 1开票 2不开票 */
    private Integer isInvoice;
    /** 1次结 2月结，创建时取客户默认值，单据可覆盖 */
    private Integer settleType;
    /** 月结对账日，创建时取客户默认值，单据可覆盖 */
    private Integer settleDay;
    private BigDecimal taxRate;
    private BigDecimal urgentFee;
    private BigDecimal palletFee;
    private BigDecimal loadingFee;
    private BigDecimal freightFee;
    private BigDecimal otherFee;

    private BigDecimal processAmountNoTax;
    private BigDecimal processAmountTax;
    private BigDecimal extraAmountNoTax;
    private BigDecimal extraAmountTax;
    private BigDecimal totalAmountNoTax;
    private BigDecimal totalAmountTax;
    private BigDecimal totalProcessAmount;
    private BigDecimal totalExtraAmount;
    private BigDecimal totalAmount;

    private BigDecimal totalOriginalWeight;
    private BigDecimal totalOriginalTon;
    private BigDecimal totalFinishWeight;

    /** 工序总道数 */
    private Integer totalStepCount;
    /** 0无追加工序 1有追加 */
    private Integer hasExtraStep;
    private Integer actualTotalKnife;

    /** 0草稿 1待下发 2加工中 3待回录 4已完成 5已结算 6已作废 */
    private Integer orderStatus;
    /** 0未打印 1已打印 */
    private Integer printStatus;
    private Integer printCount;
    private LocalDateTime lastPrintTime;
    private String lastPrintUser;
    private LocalDateTime backRecordTime;
    private String backRecordUser;
    private LocalDateTime voidTime;
    private String voidUser;
    private String voidReason;

    /** 0单一工艺 1混合锯纸+复卷 */
    private Integer isMixProcess;
    /** 下发快照JSON */
    private String snapPrint;
    /** 完成快照JSON */
    private String snapFinish;

    private String remark;
    private String remarkLong;

    @TableField(exist = false)
    private Integer originalRollCount;
    @TableField(exist = false)
    private Integer originalPieceCount;
    @TableField(exist = false)
    private BigDecimal originalRollWeight;
    @TableField(exist = false)
    private Integer finishRollCount;
    @TableField(exist = false)
    private BigDecimal finishRollWeight;
    @TableField(exist = false)
    private BigDecimal estimateFinishWeight;
    @TableField(exist = false)
    private BigDecimal actualFinishWeight;
    @TableField(exist = false)
    private Integer spareRollCount;
}
