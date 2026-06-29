package com.paper.mes.processorder.dto;

import lombok.Data;

@Data
public class ProcessConfigDraftVO {

    private String originalUuid;
    private Integer processMode;
    private Integer mainStepType;
    private Integer configStatus;
    private String lastError;
    private ProcessPlanDTO plan;
    private PlanPreviewVO preview;
}
