package com.paper.mes.ai.process.compile;

import java.util.List;

record ProcessAiPackagingCompilation(
        List<ProcessAiPackagingCandidate> candidates,
        List<String> errors,
        List<String> warnings) {

    ProcessAiPackagingCompilation {
        candidates = List.copyOf(candidates);
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }
}
