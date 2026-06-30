package com.paper.mes.system.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config_item")
public class SysConfigItem extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String configGroup;
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private String unit;
    private Integer sortNo;
    private Integer status;
    private Integer builtIn;
    private String remark;
}
