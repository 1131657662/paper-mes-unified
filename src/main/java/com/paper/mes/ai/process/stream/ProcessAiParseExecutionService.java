package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.intent.ProcessAiExtractionParser;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiGroupedPiecePlanGuard;
import com.paper.mes.ai.process.intent.ProcessAiIntentNormalizer;
import com.paper.mes.ai.process.intent.ProcessAiIntentValidator;
import com.paper.mes.ai.process.intent.ProcessAiSawRemainderResolver;
import com.paper.mes.ai.process.intent.ProcessAiSourceAssignmentResolver;
import com.paper.mes.ai.process.model.ProcessAiModelRetryExecutor;
import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.ai.process.model.ProcessAiModelResult;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import com.paper.mes.ai.process.prompt.ProcessAiPromptAssembler;
import com.paper.mes.ai.process.prompt.ProcessAiPromptBundle;
import com.paper.mes.ai.process.prompt.ProcessAiPromptContext;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ProcessAiParseExecutionService {

    private final ProcessAiPromptAssembler promptAssembler;
    private final ProcessAiModelRetryExecutor modelExecutor;
    private final ProcessAiExtractionParser extractionParser;
    private final ProcessAiIntentNormalizer intentNormalizer;
    private final ProcessAiSourceAssignmentResolver sourceAssignmentResolver;
    private final ProcessAiGroupedPiecePlanGuard groupedPiecePlanGuard;
    private final ProcessAiSawRemainderResolver sawRemainderResolver;
    private final ProcessAiIntentValidator intentValidator;
    private final ProcessAiParseCompletionService completionService;
    private final ProcessAiMessageService messageService;

    void execute(ProcessAiPreparedParse prepared, ProcessAiStreamSink sink,
                 ProcessAiCancellation cancellation) {
        ProcessAiPromptBundle prompt = promptAssembler.assemble(new ProcessAiPromptContext(
                prepared.parseId(), prepared.orderContext(), prepared.memory(),
                prepared.redaction().sanitizedText(), prepared.messages()));
        ProcessAiStreamingMessageWriter partial = new ProcessAiStreamingMessageWriter(
                prepared, messageService);
        ProcessAiModelResult model;
        try {
            model = modelExecutor.parse(prompt.prompt(), delta -> {
                partial.append(delta);
                sink.delta(delta);
            }, cancellation);
        } catch (RuntimeException exception) {
            partial.flush();
            throw exception;
        }
        ProcessAiExtractionResult normalized = intentNormalizer.normalize(
                extractionParser.parse(model.content()), prepared.redaction().sanitizedText());
        ProcessAiExtractionResult assigned = sourceAssignmentResolver.resolve(
                normalized, prepared.orderContext());
        ProcessAiExtractionResult grouped = groupedPiecePlanGuard.resolve(
                assigned, prepared.orderContext());
        ProcessAiExtractionResult extraction = sawRemainderResolver.resolve(
                grouped, prepared.orderContext(), prepared.redaction().sanitizedText());
        requireParseId(prepared.parseId(), extraction.parseId());
        intentValidator.validate(extraction, prepared.orderContext());
        ProcessAiModelExecution execution = new ProcessAiModelExecution(prompt, model, extraction);
        sink.result(completionService.complete(prepared, execution));
    }

    private void requireParseId(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new ProcessAiProviderException(
                    "AI_MODEL_PARSE_ID_MISMATCH", false, "AI解析结果请求标识不匹配");
        }
    }
}
