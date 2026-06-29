package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DraftProgressDTO {

    @Min(value = 0, message = "步骤不能小于0")
    private Integer currentStep;
}
