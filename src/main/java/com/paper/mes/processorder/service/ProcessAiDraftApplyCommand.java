package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;

import java.util.List;

public record ProcessAiDraftApplyCommand(
        String orderUuid,
        int expectedVersion,
        String parseId,
        String aiRequirementJson,
        String finalCustomerRequirement,
        List<String> acceptedFieldPaths,
        ProcessAiCompilationResult compilation) {

    public ProcessAiDraftApplyCommand {
        acceptedFieldPaths = List.copyOf(acceptedFieldPaths);
    }
}
