package com.paper.mes.ai.process.intent;

import jakarta.validation.constraints.Size;

public record ProcessAiLabelRequirement(
        boolean required,
        @Size(max = 500) String text,
        boolean createsServiceStep) {
}
