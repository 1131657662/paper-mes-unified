package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;

import java.time.LocalDateTime;
import java.util.Map;

record ProcessAiConfirmationMaterial(
        Map<String, ProcessAiCompiledPlan> plans,
        String planHash,
        int nextVersion,
        LocalDateTime confirmedAt) {
}
