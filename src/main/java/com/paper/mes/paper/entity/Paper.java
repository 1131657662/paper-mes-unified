package com.paper.mes.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 纸张档案 sys_paper。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_paper")
public class Paper extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String paperCode;
    private String paperName;
    /** 常用克重 g/㎡ */
    private Integer gramWeight;
    private String paperType;
    private String remark;
}
