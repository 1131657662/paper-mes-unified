package com.paper.mes.ai.memory.candidate;

record ProjectMemoryCandidateEvidenceWrite(
        String orderUuid,
        String parseId,
        String sourceType,
        String phrase,
        String contextJson,
        String proposedValueJson,
        String finalValueJson,
        String differenceJson,
        Boolean previewReady,
        String createdBy,
        String evidenceHash) {
}
