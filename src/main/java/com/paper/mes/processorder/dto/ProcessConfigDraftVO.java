package com.paper.mes.processorder.dto;

import lombok.Data;

@Data
public class ProcessConfigDraftVO {

    private String originalUuid;
    private Integer processMode;
    private Integer mainStepType;
    private Integer configStatus;
    private String lastError;
    private String configType;
    private ProcessPlanDTO plan;
    private PlanPreviewVO preview;
    private ProcessRoutePreviewDTO route;
    private ProcessRoutePreviewVO routePreview;
}
