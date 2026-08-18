package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;

record ProcessAiConfirmationPreparation(
        String userUuid,
        ProcessAiConfirmationLoad load,
        ProcessAiOrderContext context,
        String customerRequirement,
        ProcessTextRedactionResult redaction) {

    boolean isReplay() {
        return load.isReplay();
    }
}
