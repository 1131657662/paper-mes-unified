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
    private Integer stepSort;
    /** 1锯纸 2复卷 */
    private Integer stepType;
    private String stepName;
    /** 1本卷主工艺 0车间追加工序 */
    private Integer isMain;
    /** 锯纸专用：实际加工刀数 */
    private Integer knifeCount;
    /** 复卷专用：加工吨位 */
    private BigDecimal processWeight;
    private BigDecimal unitPrice;
    private BigDecimal stepAmount;
    private BigDecimal lossWeight;
    private String operator;
    private String remark;
}
