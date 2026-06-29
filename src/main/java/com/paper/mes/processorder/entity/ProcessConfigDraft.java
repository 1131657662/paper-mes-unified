package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新建加工单向导的单卷工艺配置草稿。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_config_draft")
public class ProcessConfigDraft extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String orderUuid;
    private String originalUuid;
    private Integer processMode;
    private Integer mainStepType;
    private String configJson;
    private String previewJson;
    /** 0未完成 1可提交 */
    private Integer configStatus;
    private String lastError;
}
