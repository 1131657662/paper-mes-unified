package com.paper.mes.system.user.dto;

import lombok.Data;

@Data
public class UserQuery {

    private String keyword;
    private String roleCode;
    private Integer status;
    private long current = 1;
    private long size = 10;
}
