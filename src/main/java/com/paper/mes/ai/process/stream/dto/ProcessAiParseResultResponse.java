package com.paper.mes.ai.process.stream.dto;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.compile.ProcessAiRollConfiguration;
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
        OffsetDateTime expiresAt,
        String resultKind,
        String dialogueState,
        ProcessAiUnderstandingResult understanding,
        List<ProcessAiClarificationQuestion> clarificationQuestions,
        List<String> requiredDefaultIds,
        String previewHash) {

    public ProcessAiParseResultResponse(
            String conversationId, String parseId, int parseRevision, int expectedVersion,
            Integer nextVersion, String status, ProcessAiReviewBaseline baseline,
            ProcessAiExtractionResult result, CompiledStatus compiled,
            OffsetDateTime expiresAt) {
        this(conversationId, parseId, parseRevision, expectedVersion, nextVersion, status,
                baseline, result, compiled, expiresAt,
                result == null ? "UNDERSTANDING" : "EXTRACTION",
                "CLARIFICATION".equals(status) ? "CLARIFYING" :
                        "READY".equals(status) ? "PREVIEW_READY" : "COMPLETED",
                null, List.of(), List.of(), null);
    }

    public ProcessAiParseResultResponse {
        clarificationQuestions = clarificationQuestions == null
                ? List.of() : List.copyOf(clarificationQuestions);
        requiredDefaultIds = requiredDefaultIds == null
                ? List.of() : List.copyOf(requiredDefaultIds);
    }

    public record CompiledStatus(
            boolean eligible,
            List<ProcessAiRollConfiguration> rollConfigurations,
            List<ProcessAiCompiledPlan> plans,
            List<ProcessAiPackagingCandidate> packagingCandidates,
            List<String> errors,
            List<String> warnings) {

        public CompiledStatus {
            rollConfigurations = List.copyOf(rollConfigurations);
            plans = List.copyOf(plans);
            packagingCandidates = List.copyOf(packagingCandidates);
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }

        public CompiledStatus(boolean eligible, List<ProcessAiCompiledPlan> plans,
                              List<ProcessAiPackagingCandidate> packagingCandidates,
                              List<String> errors, List<String> warnings) {
            this(eligible, List.of(), plans, packagingCandidates, errors, warnings);
        }
    }
}
