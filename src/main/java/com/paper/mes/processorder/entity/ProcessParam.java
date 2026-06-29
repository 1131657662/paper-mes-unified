package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 复卷工艺参数表 biz_process_param。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_param")
public class ProcessParam extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private String originalUuid;
    private String stepUuid;
    private Integer paramMode;
    private Integer layerSort;
    private Integer outDiameter;
    private Integer coreDiameter;
    private Integer layerWidth;
    private BigDecimal areaValue;
    private BigDecimal areaRatio;
    private BigDecimal splitRatio;
    private String paramJson;
    private String remark;
}
