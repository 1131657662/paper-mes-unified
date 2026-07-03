package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工艺阶段产出表：用于表达后续/多段工艺中的中间产出和最终产出。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_stage_output")
public class ProcessStageOutput extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private String originalUuid;
    private String stepUuid;
    private String parentOutputUuid;
    private Integer stageLevel;
    private Integer outputSort;
    /** 1中间产出 2最终产出 */
    private Integer outputType;
    /** 1计划 2已被下道消耗 3已生成成品卷 4作废 */
    private Integer outputStatus;
    private String outputNo;
    private String finishRollUuid;
    private String paperName;
    private Integer gramWeight;
    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    private BigDecimal estimateWeight;
    private BigDecimal actualWeight;
    private Integer sourceStepType;
    private String sourceSummary;
    private String remark;
}
