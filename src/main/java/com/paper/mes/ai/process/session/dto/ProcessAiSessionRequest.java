package com.paper.mes.ai.process.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProcessAiSessionRequest(
        @NotNull(message = "草稿版本不能为空")
        @Min(value = 0, message = "草稿版本不能小于0")
        Integer expectedVersion,
        @NotNull(message = "当前步骤不能为空")
        @Min(value = 3, message = "AI工艺助手只允许在第3、4步使用")
        @Max(value = 4, message = "AI工艺助手只允许在第3、4步使用")
        Integer currentStep) {
}
