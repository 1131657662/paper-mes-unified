package com.paper.mes.ai.memory.candidate;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.context.ProcessAiReviewBaseline;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;

import java.util.List;

public record ProjectMemoryCandidateConfirmedEvent(
        String orderUuid,
        String parseId,
        String projectMemoryVersion,
        String customerRequirement,
        String confirmedBy,
        ProcessAiExtractionResult extraction,
        List<String> acceptedFieldPaths,
        ProcessAiCompilationResult compilation,
        ProcessAiReviewBaseline baseline) {

    public ProjectMemoryCandidateConfirmedEvent {
        acceptedFieldPaths = List.copyOf(acceptedFieldPaths);
    }
}
