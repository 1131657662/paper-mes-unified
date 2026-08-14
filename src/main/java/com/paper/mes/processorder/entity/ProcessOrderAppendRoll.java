package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_order_append_roll")
public class ProcessOrderAppendRoll extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String sessionUuid;
    private Integer rowSort;
    private String extraNo;
    private String rollNo;
    private String paperName;
    private Integer gramWeight;
    private Integer originalWidth;
    private Integer originalDiameter;
    private Integer coreDiameter;
    private Integer originalLength;
    private BigDecimal rollWeight;
    private String weightStatus;
    private Integer pieceNum;
    private String batchNo;
    private String damageDesc;
    private Integer processMode;
    private Integer mainStepType;
    private String machineUuid;
    private String remark;
    private String configJson;
    private String previewJson;
    private Integer configStatus;
    private String configType;
    private String serviceStepsJson;
    private String lastError;
}
