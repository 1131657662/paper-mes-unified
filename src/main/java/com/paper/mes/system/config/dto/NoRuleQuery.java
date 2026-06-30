package com.paper.mes.system.config.dto;

import lombok.Data;

@Data
public class NoRuleQuery {
    private String keyword;
    private String bizType;
    private Integer status;
    private long current = 1;
    private long size = 10;
}
