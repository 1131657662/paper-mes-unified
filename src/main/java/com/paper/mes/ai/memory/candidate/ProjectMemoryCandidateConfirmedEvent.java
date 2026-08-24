package com.paper.mes.ai.memory.candidate;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.context.ProcessAiReviewBaseline;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiCorrection;

import java.util.List;

public record ProjectMemoryCandidateConfirmedEvent(
        String orderUuid,
        String parseId,
        String projectMemoryVersion,
        String customerRequirementHash,
        String confirmedBy,
        ProcessAiExtractionResult extraction,
        List<String> acceptedFieldPaths,
        ProcessAiCompilationResult compilation,
        ProcessAiReviewBaseline baseline,
        List<ProcessAiCorrection> corrections,
        List<String> effectiveDefaults,
        String previewHash) {

    public ProjectMemoryCandidateConfirmedEvent(
            String orderUuid, String parseId, String projectMemoryVersion,
            String customerRequirement, String confirmedBy,
            ProcessAiExtractionResult extraction, List<String> acceptedFieldPaths,
            ProcessAiCompilationResult compilation, ProcessAiReviewBaseline baseline) {
        this(orderUuid, parseId, projectMemoryVersion, customerRequirement, confirmedBy,
                extraction, acceptedFieldPaths, compilation, baseline, List.of(), List.of(), null);
    }

    public ProjectMemoryCandidateConfirmedEvent {
        customerRequirementHash = stableHash(customerRequirementHash);
        acceptedFieldPaths = List.copyOf(acceptedFieldPaths);
        corrections = corrections == null ? List.of() : List.copyOf(corrections);
        effectiveDefaults = effectiveDefaults == null ? List.of() : List.copyOf(effectiveDefaults);
    }

    private static String stableHash(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}")
                ? value.toLowerCase(java.util.Locale.ROOT)
                : ProcessAiAuditHasher.sha256(value);
    }
}
