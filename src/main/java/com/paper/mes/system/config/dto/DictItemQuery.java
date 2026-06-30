package com.paper.mes.system.config.dto;

import lombok.Data;

@Data
public class DictItemQuery {
    private String keyword;
    private String dictType;
    private Integer status;
    private long current = 1;
    private long size = 10;
}
