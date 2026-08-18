package com.paper.mes.ai.process.stream.dto;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.context.ProcessAiReviewBaseline;

import java.time.OffsetDateTime;
import java.util.List;

public record ProcessAiParseResultResponse(
        String conversationId,
        String parseId,
        int parseRevision,
        int expectedVersion,
        Integer nextVersion,
        String status,
        ProcessAiReviewBaseline baseline,
        ProcessAiExtractionResult result,
        CompiledStatus compiled,
        OffsetDateTime expiresAt) {

    public record CompiledStatus(
            boolean eligible,
            List<ProcessAiCompiledPlan> plans,
            List<ProcessAiPackagingCandidate> packagingCandidates,
            List<String> errors,
            List<String> warnings) {

        public CompiledStatus {
            plans = List.copyOf(plans);
            packagingCandidates = List.copyOf(packagingCandidates);
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }
    }
}
