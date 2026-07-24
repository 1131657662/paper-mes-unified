package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成品明细表 biz_finish_roll（全局唯一卷号）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_finish_roll")
public class FinishRoll extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private Integer rowSort;
    /** 成品全局唯一编号：1大写字母+6位数字 */
    private String finishRollNo;
    private String finishInnerNo;
    /** 1预生成 2已使用 3作废 */
    private Integer rollNoStatus;
    /** 0正式成品号 1备用冗余卷号 */
    private Integer isSpare;
    private String paperName;
    private String customerPaperName;
    private Integer gramWeight;
    private Integer customerGramWeight;
    private Integer finishWidth;
    private Integer customerFinishWidth;
    private BigDecimal customerDisplayWeight;
    private String customerSpecOverrideReason;
    private String customerSpecOverrideBy;
    private LocalDateTime customerSpecOverrideAt;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    /** 1加工产出 2原纸直发 3仅附加工艺产出 */
    private Integer sourceType;
    private BigDecimal estimateWeightSnap;
    private BigDecimal estimateWeight;
    private BigDecimal actualWeight;
    private BigDecimal remainingWeight;
    private BigDecimal diameterRatio;
    private Integer trimWidthShare;
    private BigDecimal trimWeightShare;
    private Integer isWeightAdjust;
    private String weightAdjustReason;
    private BigDecimal weightDiff;
    private Integer isManualEdit;
    private Integer isRemain;
    private Integer isAbnormal;
    private String abnormalType;
    private BigDecimal scrapWeight;
    /** 1待检 2合格 3不合格 4让步接收 */
    private Integer qualityStatus;
    /** 1待入库 2已入库 3已出库 4报废 */
    private Integer finishStatus;
    /** 首次正式入库时间；历史数据无法可靠推断时允许为空。 */
    private LocalDateTime stockInTime;
    private String warehouseUuid;
    private String originalRollNos;
    private String actualRemark;
    private String remark;
}
