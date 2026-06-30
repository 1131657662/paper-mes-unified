package com.paper.mes.system.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserVO {

    private String uuid;
    private String username;
    private String realName;
    private String roleCode;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String remark;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
