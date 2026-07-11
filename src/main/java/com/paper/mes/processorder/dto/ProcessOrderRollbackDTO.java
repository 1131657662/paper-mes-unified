package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProcessOrderRollbackDTO {

    @NotBlank(message = "回退原因不能为空")
    @Size(max = 255, message = "回退原因不能超过255个字符")
    private String reason;
}
