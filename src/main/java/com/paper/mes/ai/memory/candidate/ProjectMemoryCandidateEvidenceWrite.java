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
        String evidenceHash,
        String orderRefHash,
        String parseRefHash,
        String auditContextCiphertext,
        String auditContextHash) {

    ProjectMemoryCandidateEvidenceWrite(
            String orderUuid, String parseId, String sourceType, String phrase,
            String contextJson, String proposedValueJson, String finalValueJson,
            String differenceJson, Boolean previewReady, String createdBy, String evidenceHash) {
        this(orderUuid, parseId, sourceType, phrase, contextJson, proposedValueJson,
                finalValueJson, differenceJson, previewReady, createdBy, evidenceHash,
                null, null, null, null);
    }
}
