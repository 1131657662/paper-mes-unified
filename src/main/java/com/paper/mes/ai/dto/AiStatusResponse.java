package com.paper.mes.ai.dto;

import com.paper.mes.ai.config.AiDataMode;
import com.paper.mes.ai.config.AiProvider;

public record AiStatusResponse(boolean enabled, AiDataMode dataMode, String rulesVersion,
                               boolean rulesReady, AiProvider provider) {
}
