package com.paper.mes.ai.process.compile;

import java.util.List;

public record ProcessAiCompilationResult(
        boolean eligible,
        List<ProcessAiCompiledPlan> plans,
        List<ProcessAiPackagingCandidate> packagingCandidates,
        List<String> errors,
        List<String> warnings) {

    public ProcessAiCompilationResult {
        plans = List.copyOf(plans);
        packagingCandidates = List.copyOf(packagingCandidates);
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }
}
