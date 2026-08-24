package com.paper.mes.ai.process.compile;

import java.util.List;

public record ProcessAiCompilationResult(
        boolean eligible,
        List<ProcessAiRollConfiguration> rollConfigurations,
        List<ProcessAiCompiledPlan> plans,
        List<ProcessAiPackagingCandidate> packagingCandidates,
        List<String> errors,
        List<String> warnings) {

    public ProcessAiCompilationResult {
        rollConfigurations = List.copyOf(rollConfigurations);
        plans = List.copyOf(plans);
        packagingCandidates = List.copyOf(packagingCandidates);
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    /** Compatibility constructor for schema-v1 tests and callers without Step 3 output. */
    public ProcessAiCompilationResult(boolean eligible,
                                      List<ProcessAiCompiledPlan> plans,
                                      List<ProcessAiPackagingCandidate> packagingCandidates,
                                      List<String> errors,
                                      List<String> warnings) {
        this(eligible, List.of(), plans, packagingCandidates, errors, warnings);
    }
}
