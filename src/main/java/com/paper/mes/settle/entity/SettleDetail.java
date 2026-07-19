package com.paper.mes.settle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 结算单加工单明细 biz_settle_detail。一条对应一张被结算的加工单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settle_detail")
public class SettleDetail extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String settleUuid;
    private String orderUuid;
    private String orderNo;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    /** 优惠前标准加工费。 */
    private BigDecimal standardProcessAmount;
    /** 最终加工费减标准加工费。 */
    private BigDecimal pricingAdjustmentAmount;
    private String pricingAdjustmentReason;
    private BigDecimal extraAmount;
    /** 本单计入金额 = 该加工单 total_amount */
    private BigDecimal orderAmount;
    private String remark;
}
