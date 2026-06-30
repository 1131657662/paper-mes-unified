package com.paper.mes.system.config.dto;

import lombok.Data;

@Data
public class ConfigItemQuery {
    private String keyword;
    private String configGroup;
    private Integer status;
    private long current = 1;
    private long size = 10;
}
