package com.paper.mes.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUser {

    private String uuid;
    private String username;
    private String realName;
    private String roleCode;
}
