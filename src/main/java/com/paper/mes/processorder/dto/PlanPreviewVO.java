package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanPreviewVO {

    private String originalUuid;
    private Integer processMode;
    private Integer mainStepType;
    private Integer rewindMode;
    private Integer finishCount;
    private Integer trimCount;
    private Integer spareCount;
    private BigDecimal totalEstimateWeight;
    private BigDecimal totalTrimWeight;
    private String summary;
    private boolean ready;
    private List<String> errors = new ArrayList<>();
    private List<FinishPreviewVO.SegmentPreview> segments = new ArrayList<>();
    private List<FinishPreviewVO.FinishItemPreview> finishes = new ArrayList<>();
}
