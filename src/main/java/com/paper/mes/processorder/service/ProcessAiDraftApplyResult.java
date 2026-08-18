package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessAiDraftApplyResult(
        int nextVersion,
        Map<String, ProcessAiCompiledPlan> plans,
        List<ProcessAiPackagingCandidate> packagingCandidates) {

    public ProcessAiDraftApplyResult {
        plans = Collections.unmodifiableMap(new LinkedHashMap<>(plans));
        packagingCandidates = List.copyOf(packagingCandidates);
    }
}
