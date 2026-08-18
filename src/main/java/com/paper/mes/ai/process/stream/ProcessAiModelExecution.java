package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.model.ProcessAiModelResult;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;

record ProcessAiModelExecution(
        ProcessAiPromptBundle promptBundle,
        ProcessAiModelResult modelResult,
        ProcessAiExtractionResult extraction) {
}
