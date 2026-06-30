package com.paper.mes.system.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_no_rule")
public class SysNoRule extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String bizType;
    private String ruleName;
    private String prefix;
    private Integer patternType;
    private String datePattern;
    private Integer serialLength;
    private Integer resetCycle;
    private Integer status;
    private String remark;
}
