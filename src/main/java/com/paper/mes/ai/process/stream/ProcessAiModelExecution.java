package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult;
import com.paper.mes.ai.process.model.ProcessAiModelResult;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;

record ProcessAiModelExecution(
        ProcessAiPromptBundle promptBundle,
        ProcessAiModelResult modelResult,
        ProcessAiExtractionResult extraction,
        ProcessAiUnderstandingResult understanding) {

    ProcessAiModelExecution(ProcessAiPromptBundle promptBundle,
                             ProcessAiModelResult modelResult,
                             ProcessAiExtractionResult extraction) {
        this(promptBundle, modelResult, extraction, null);
    }

    boolean isUnderstanding() {
        return understanding != null;
    }
}
