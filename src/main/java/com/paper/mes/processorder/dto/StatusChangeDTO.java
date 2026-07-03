package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 状态变更入参（通用转态接口）。
 */
@Data
public class StatusChangeDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    @Size(max = 255, message = "状态变更原因不能超过255个字符")
    private String reason;
}
