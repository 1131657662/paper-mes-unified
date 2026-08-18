package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;

record ProcessAiConfirmationWriteCommand(
        ProcessAiConfirmationLoad load,
        ProcessAiCompilationResult compilation,
        String confirmedBy,
        String customerRequirement) {
}
