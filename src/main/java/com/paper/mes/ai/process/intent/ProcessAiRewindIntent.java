package com.paper.mes.ai.process.intent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProcessAiRewindIntent(
        @NotBlank @Pattern(regexp = "CHANGE_WIDTH|CHANGE_DIAMETER|CHANGE_WIDTH_AND_DIAMETER|LAYERED|MULTI_SOURCE|KEEP_SPEC")
        String modeIntent,
        @Valid ProcessAiDiameterRule diameterRule,
        @Valid ProcessAiMeasurement core,
        @Valid ProcessAiWidthRule widthRule,
        @Valid ProcessAiQuantityIntent quantityIntent) {

    public ProcessAiRewindIntent(String modeIntent, ProcessAiDiameterRule diameterRule,
                                 ProcessAiMeasurement core, ProcessAiWidthRule widthRule) {
        this(modeIntent, diameterRule, core, widthRule, null);
    }
}
