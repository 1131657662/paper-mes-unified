package com.paper.mes.ai.process.parse.dto;

import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProcessAiConfirmResponse(
        String conversationId,
        String parseId,
        int parseRevision,
        int expectedVersion,
        int nextVersion,
        String status,
        List<String> acceptedFieldPaths,
        Map<String, ProcessAiCompiledPlan> plans,
        List<ProcessAiPackagingCandidate> packagingCandidates,
        List<String> warnings,
        String remarkLong,
        String planHash) {

    public ProcessAiConfirmResponse {
        acceptedFieldPaths = List.copyOf(acceptedFieldPaths);
        plans = Collections.unmodifiableMap(new LinkedHashMap<>(plans));
        packagingCandidates = List.copyOf(packagingCandidates);
        warnings = List.copyOf(warnings);
    }
}
