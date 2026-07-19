package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工序明细表 biz_process_step（工艺唯一来源）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_step")
public class ProcessStep extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private String originalUuid;
    /** 1原纸 2上一阶段产出 */
    private Integer inputType;
    private String inputOutputUuid;
    private Integer stageLevel;
    private String parentStepUuid;
    private Integer stepSort;
    /** 1锯纸 2复卷 */
    private Integer stepType;
    private String stepName;
    private String machineUuid;
    private String machineNameSnap;
    /** 1本卷主工艺 0车间追加工序 */
    private Integer isMain;
    /** 锯纸专用：实际加工刀数 */
    private Integer knifeCount;
    /** 复卷专用：加工吨位 */
    private BigDecimal processWeight;
    /** 客户档案/加工单解析得到的标准单价快照。 */
    private BigDecimal unitPrice;
    /** 人工核定单价；为空时沿用标准单价。 */
    private BigDecimal billingUnitPrice;
    private BigDecimal stepAmount;
    /** 1标准计价 2按指定数量计价 3固定金额 4免收。 */
    private Integer billingMode;
    /** 未应用优惠前的标准计费数量，锯纸为刀数，复卷为吨位。 */
    private BigDecimal standardQuantity;
    /** 最终计费数量；标准模式下与 standard_quantity 一致。 */
    private BigDecimal billingQuantity;
    /** 固定金额模式下的最终金额。 */
    private BigDecimal billingAmount;
    /** 未应用调整前的标准工序金额。 */
    private BigDecimal standardStepAmount;
    /** 最终金额减标准金额，可为负数表示优惠。 */
    private BigDecimal pricingAdjustmentAmount;
    private String pricingAdjustmentReason;
    private String pricingAdjustedBy;
    private java.time.LocalDateTime pricingAdjustedAt;
    private String pricingAdjustmentBatchId;
    private BigDecimal lossWeight;
    private String operator;
    private String remark;
}
