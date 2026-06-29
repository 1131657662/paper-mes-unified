package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewindSourcePlanDTO {

    private String originalUuid;
    private Integer sourceSort;
    private BigDecimal shareRatio;
    private BigDecimal consumeRatio;
    private BigDecimal shareWeight;
    private String remark;
}
