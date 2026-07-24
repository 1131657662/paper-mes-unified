package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DraftProgressDTO {

    @jakarta.validation.constraints.NotNull(message = "草稿版本不能为空")
    @Min(value = 0, message = "草稿版本不能小于0")
    private Integer expectedVersion;

    @Min(value = 0, message = "步骤不能小于0")
    private Integer currentStep;
}
