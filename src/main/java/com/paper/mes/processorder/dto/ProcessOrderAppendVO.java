package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessOrderAppendVO {
    private String sessionUuid;
    private String orderUuid;
    private String orderNo;
    private Integer baseOrderVersion;
    private Integer currentOrderVersion;
    private Integer sessionVersion;
    private String status;
    private String reason;
    private List<Roll> rolls = new ArrayList<>();

    @Data
    public static class Roll {
        private String uuid;
        private Integer rowSort;
        private String extraNo;
        private String rollNo;
        private String paperName;
        private Integer gramWeight;
        private Integer originalWidth;
        private Integer originalDiameter;
        private Integer coreDiameter;
        private Integer originalLength;
        private BigDecimal rollWeight;
        private String weightStatus;
        private Integer pieceNum;
        private String batchNo;
        private String damageDesc;
        private Integer processMode;
        private Integer mainStepType;
        private String machineUuid;
        private String remark;
        private Integer configStatus;
        private String configType;
        private List<ProcessStepDTO> serviceSteps = new ArrayList<>();
        private String lastError;
        private FinishConfigSaveDTO config;
        private String previewJson;
        private PlanPreviewVO preview;
        private ProcessRoutePreviewDTO route;
        private ProcessRoutePreviewVO routePreview;
    }

    @Data
    public static class CommitResult {
        private String sessionUuid;
        private String orderUuid;
        private Integer orderVersion;
        private List<String> rollUuids = new ArrayList<>();
        private List<String> finishRollNos = new ArrayList<>();
    }
}
