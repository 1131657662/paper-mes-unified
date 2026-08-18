package com.paper.mes.ai.process.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProcessAiMemoryRefreshRequest(
        @NotNull(message = "草稿版本不能为空")
        @Min(value = 0, message = "草稿版本不能小于0")
        Integer expectedVersion) {
}
