package com.paper.mes.ai.process.parse.dto;

import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;

public record ProcessAiPendingPackagingCandidate(
        String parseId,
        ProcessAiPackagingCandidate candidate) {
}
