package com.paper.mes.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthUserVO {

    private String uuid;
    private String username;
    private String realName;
    private String roleCode;
    private String accessToken;
    private List<String> permissions;
}
