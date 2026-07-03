package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多段工艺输入关联表：一条工序可消费多个上一阶段产出。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_stage_input_rel")
public class ProcessStageInputRel extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private String originalUuid;
    private String stepUuid;
    private String inputOutputUuid;
    private String sourceStepUuid;
    private Integer inputSort;
    private Integer stageLevel;
}
