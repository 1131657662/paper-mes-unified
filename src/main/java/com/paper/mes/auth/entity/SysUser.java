package com.paper.mes.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String username;
    private String passwordHash;
    private String realName;
    private String roleCode;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String remark;
}
