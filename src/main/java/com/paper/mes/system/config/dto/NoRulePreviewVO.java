package com.paper.mes.system.config.dto;

import lombok.Data;

@Data
public class NoRulePreviewVO {
    private String bizType;
    private String exampleNo;
    private String sequenceKey;
    private Long currentValue;
    private Long nextValue;
}
