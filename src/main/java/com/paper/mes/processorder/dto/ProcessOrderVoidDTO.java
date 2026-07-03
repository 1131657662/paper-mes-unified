package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加工单整单作废入参。
 */
@Data
public class ProcessOrderVoidDTO {

    @NotBlank(message = "作废原因不能为空")
    @Size(max = 255, message = "作废原因不能超过255个字符")
    private String reason;
}
