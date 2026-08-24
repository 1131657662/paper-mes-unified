package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;

import java.util.List;

record ProcessAiConfirmationLoad(
        ProcessAiParseRecord record,
        ProcessAiExtractionResult extraction,
        List<String> acceptedFieldPaths,
        String applyIdempotencyKey,
        ProcessAiConfirmResponse replay,
        String previewHash,
        List<String> acknowledgedDefaultIds) {

    ProcessAiConfirmationLoad(ProcessAiParseRecord record,
                              ProcessAiExtractionResult extraction,
                              List<String> acceptedFieldPaths,
                              String applyIdempotencyKey,
                              ProcessAiConfirmResponse replay) {
        this(record, extraction, acceptedFieldPaths, applyIdempotencyKey, replay,
                record.previewHash(), List.of());
    }

    boolean isReplay() {
        return replay != null;
    }
}
