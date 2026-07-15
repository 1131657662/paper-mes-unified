package com.paper.mes.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DataHealthRepairRequest(
        @NotBlank(message = "修复原因不能为空")
        @Size(max = 255, message = "修复原因不能超过255个字")
        String reason,
        @NotBlank(message = "请输入业务单号确认修复")
        @Size(max = 50, message = "确认内容过长")
        String confirmation
) {
}
