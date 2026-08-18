package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;

import java.util.List;

record ProcessAiConfirmationLoad(
        ProcessAiParseRecord record,
        ProcessAiExtractionResult extraction,
        List<String> acceptedFieldPaths,
        String applyIdempotencyKey,
        ProcessAiConfirmResponse replay) {

    boolean isReplay() {
        return replay != null;
    }
}
