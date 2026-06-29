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
 * 原纸明细表 biz_original_roll（单卷维度）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_original_roll")
public class OriginalRoll extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    /** 单据内排序行号 */
    private Integer rowSort;
    /** 客户内部编号 */
    private String extraNo;
    /** 来料母卷号 */
    private String rollNo;
    private String paperName;
    /** 来料标称克重 g/㎡ */
    private Integer gramWeight;
    private Integer actualGramWeight;
    /** 标称门幅 mm */
    private Integer originalWidth;
    private Integer actualWidth;
    /** 原卷直径 英寸 */
    private Integer originalDiameter;
    /** 纸芯直径 英寸 */
    private Integer coreDiameter;
    /** 来料长度 米 */
    private Integer originalLength;

    /** 标称单件重量 kg */
    private BigDecimal rollWeight;
    /** 实际重量 kg（计费基准） */
    private BigDecimal actualWeight;
    /** 件数，默认1 */
    private Integer pieceNum;
    /** 标称总重=件重*件数 */
    private BigDecimal totalWeight;

    private String batchNo;
    private String damageDesc;
    /** 多图片路径数组JSON */
    private String damageImages;

    /** 1标准加工 2现场定尺 3不加工直发 */
    private Integer processMode;
    /** 主工艺类型：1锯纸 2复卷 */
    private Integer mainStepType;
    /** 1待加工 2加工中 3完成 4直发 5报废 */
    private Integer rollStatus;
    /** 0未复核 1复核完成 */
    private Integer isChecked;
    private String checkUser;
    private LocalDateTime checkTime;
    private String machineUuid;
    private String operator;

    /** 本卷加工费合计 */
    private BigDecimal processAmount;
    private BigDecimal totalLossWeight;
    private BigDecimal totalLossRatio;

    /** 冗余客户名 */
    private String customerName;
    /** 冗余加工单号 */
    private String orderNo;
    private String remark;
}
