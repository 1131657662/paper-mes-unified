package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RewindSegmentPlanDTO {

    private Integer segmentSort;
    private BigDecimal segmentRatio;
    private Integer targetDiameter;
    private Integer finishCoreDiameter;
    private Integer repeatCount;

    @Valid
    private List<RewindSourcePlanDTO> sources;

    @Valid
    private List<RewindLayoutItemPlanDTO> layoutItems;
}
