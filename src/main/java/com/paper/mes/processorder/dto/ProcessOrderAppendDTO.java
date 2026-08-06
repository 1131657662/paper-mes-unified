package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

public final class ProcessOrderAppendDTO {
    private ProcessOrderAppendDTO() {
    }

    @Data
    public static class Create {
        @NotNull
        private Integer expectedOrderVersion;
        private String reason;
    }

    @Data
    public static class RollBatch {
        @NotNull
        private Integer expectedSessionVersion;
        @Valid
        @NotNull
        private List<AppendRoll> rolls;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AppendRoll extends OriginalRollDTO {
        /** Only service steps are accepted here; the server binds their source UUID. */
        private List<ProcessStepDTO> serviceSteps;
    }

    @Data
    public static class ProcessSettings {
        @NotNull
        private Integer expectedSessionVersion;
        @Valid
        @NotNull
        private List<RollProcess> rolls;
    }

    @Data
    public static class RollProcess {
        @NotBlank
        private String rollUuid;
        @NotNull
        private Integer processMode;
        private Integer mainStepType;
        private String machineUuid;
    }

    @Data
    public static class PlanSave {
        @NotNull
        private Integer expectedSessionVersion;
        @NotNull
        private String rollUuid;
        @NotNull
        private FinishConfigSaveDTO config;
        private String configType = "singlePlan";
        private String previewJson;
    }

    @Data
    public static class PlanPreview {
        @NotNull
        private Integer expectedSessionVersion;
        @Valid
        @NotNull
        private com.paper.mes.processorder.dto.ProcessPlanDTO plan;
    }

    @Data
    public static class Preview {
        @NotNull
        private Integer expectedSessionVersion;
    }

    @Data
    public static class Commit {
        @NotNull
        private Integer expectedOrderVersion;
        @NotBlank
        private String requestId;
    }
}
