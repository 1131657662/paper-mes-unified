package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;

public record ProcessAiAncillaryRequirements(
        @Valid ProcessAiLabelRequirement label,
        @Valid ProcessAiPackagingRequirement packaging) {
}
