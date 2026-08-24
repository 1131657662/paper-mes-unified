package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.intent.ProcessAiExtractionParser;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiEvidenceVerifier;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.intent.ProcessAiGroupedPiecePlanGuard;
import com.paper.mes.ai.process.intent.ProcessAiIntentNormalizer;
import com.paper.mes.ai.process.intent.ProcessAiIntentValidator;
import com.paper.mes.ai.process.intent.ProcessAiUnderstandingParser;
import com.paper.mes.ai.process.intent.ProcessAiUnderstandingEvidenceSanitizer;
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
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ProcessAiParseExecutionService {

    private final ProcessAiPromptAssembler promptAssembler;
    private final ProcessAiModelRetryExecutor modelExecutor;
    private final ProcessAiExtractionParser extractionParser;
    private final ProcessAiEvidenceVerifier evidenceVerifier;
    private final ProcessAiUnderstandingParser understandingParser;
    private final ProcessAiUnderstandingEvidenceSanitizer understandingEvidenceSanitizer;
    private final ProcessAiResultKindDetector resultKindDetector;
    private final ProcessAiIntentNormalizer intentNormalizer;
    private final ProcessAiSourceAssignmentResolver sourceAssignmentResolver;
    private final ProcessAiGroupedPiecePlanGuard groupedPiecePlanGuard;
    private final ProcessAiSawRemainderResolver sawRemainderResolver;
    private final ProcessAiIntentValidator intentValidator;
    private final ProcessAiParseCompletionService completionService;
    private final ProcessAiMessageService messageService;

    ProcessAiParseExecutionService(ProcessAiPromptAssembler promptAssembler,
                                   ProcessAiModelRetryExecutor modelExecutor,
                                   ProcessAiExtractionParser extractionParser,
                                   ProcessAiIntentNormalizer intentNormalizer,
                                   ProcessAiSourceAssignmentResolver sourceAssignmentResolver,
                                   ProcessAiGroupedPiecePlanGuard groupedPiecePlanGuard,
                                   ProcessAiSawRemainderResolver sawRemainderResolver,
                                   ProcessAiIntentValidator intentValidator,
                                   ProcessAiParseCompletionService completionService,
                                   ProcessAiMessageService messageService) {
        this(promptAssembler, modelExecutor, extractionParser, null, null, null, null, intentNormalizer,
                sourceAssignmentResolver, groupedPiecePlanGuard, sawRemainderResolver,
                intentValidator, completionService, messageService);
    }

    @Autowired
    ProcessAiParseExecutionService(ProcessAiPromptAssembler promptAssembler,
                                   ProcessAiModelRetryExecutor modelExecutor,
                                   ProcessAiExtractionParser extractionParser,
                                   ProcessAiUnderstandingParser understandingParser,
                                   ProcessAiResultKindDetector resultKindDetector,
                                   ProcessAiUnderstandingEvidenceSanitizer understandingEvidenceSanitizer,
                                   ProcessAiEvidenceVerifier evidenceVerifier,
                                   ProcessAiIntentNormalizer intentNormalizer,
                                   ProcessAiSourceAssignmentResolver sourceAssignmentResolver,
                                   ProcessAiGroupedPiecePlanGuard groupedPiecePlanGuard,
                                   ProcessAiSawRemainderResolver sawRemainderResolver,
                                   ProcessAiIntentValidator intentValidator,
                                   ProcessAiParseCompletionService completionService,
                                   ProcessAiMessageService messageService) {
        this.promptAssembler = promptAssembler;
        this.modelExecutor = modelExecutor;
        this.extractionParser = extractionParser;
        this.evidenceVerifier = evidenceVerifier;
        this.understandingParser = understandingParser;
        this.resultKindDetector = resultKindDetector;
        this.understandingEvidenceSanitizer = understandingEvidenceSanitizer;
        this.intentNormalizer = intentNormalizer;
        this.sourceAssignmentResolver = sourceAssignmentResolver;
        this.groupedPiecePlanGuard = groupedPiecePlanGuard;
        this.sawRemainderResolver = sawRemainderResolver;
        this.intentValidator = intentValidator;
        this.completionService = completionService;
        this.messageService = messageService;
    }

    void execute(ProcessAiPreparedParse prepared, ProcessAiStreamSink sink,
                 ProcessAiCancellation cancellation) {
        ProcessAiPromptContext promptContext = new ProcessAiPromptContext(
                prepared.parseId(), prepared.reservation().parseRevision(), prepared.orderContext(),
                prepared.memory(), prepared.redaction().sanitizedText(), prepared.messages(),
                prepared.clarificationQuestion(), prepared.request().answerCode(),
                prepared.redaction().sanitizedText());
        ProcessAiPromptBundle prompt = promptAssembler.assemble(promptContext);
        ProcessAiStreamingMessageWriter partial = new ProcessAiStreamingMessageWriter(
                prepared, messageService);
        try {
            ProcessAiModelExecution execution = parseExecution(promptContext, prompt, partial,
                    prepared, sink, cancellation);
            emitValidatedModelContent(execution, partial, sink);
            if (execution.isUnderstanding()) {
                sink.result(completeUnderstandingWithPersistenceRetry(prepared, execution));
                return;
            }
            sink.result(completeWithPersistenceRetry(prepared, execution));
        } catch (RuntimeException exception) {
            partial.flush();
            throw exception;
        }
    }

    private ProcessAiParseResultResponse completeWithPersistenceRetry(
            ProcessAiPreparedParse prepared, ProcessAiModelExecution execution) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return completionService.complete(prepared, execution);
            } catch (DataAccessException exception) {
                if (attempt == 2) throw exception;
            }
        }
        throw new IllegalStateException("AI parse completion retry exhausted");
    }

    private ProcessAiParseResultResponse completeUnderstandingWithPersistenceRetry(
            ProcessAiPreparedParse prepared, ProcessAiModelExecution execution) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return completionService.completeUnderstanding(prepared, execution);
            } catch (DataAccessException exception) {
                if (attempt == 2) throw exception;
            }
        }
        throw new IllegalStateException("AI understanding completion retry exhausted");
    }

    /** Emits provider content only after the structured result has passed every server-side guard. */
    private void emitValidatedModelContent(ProcessAiModelExecution execution,
                                           ProcessAiStreamingMessageWriter partial,
                                           ProcessAiStreamSink sink) {
        String content = execution.modelResult().content();
        if (content == null || content.isEmpty()) return;
        partial.append(content);
        partial.flush();
        sink.delta(content);
    }

    private ProcessAiModelExecution parseExecution(ProcessAiPromptContext promptContext,
                                                    ProcessAiPromptBundle prompt,
                                                    ProcessAiStreamingMessageWriter partial,
                                                    ProcessAiPreparedParse prepared,
                                                    ProcessAiStreamSink sink,
                                                    ProcessAiCancellation cancellation) {
        try {
            return decode(prompt, model(prompt, partial, sink, cancellation), prepared);
        } catch (ProcessAiProviderException exception) {
            if (!"AI_MODEL_RESULT_INVALID".equals(exception.failureCode())) throw exception;
            ProcessAiPromptBundle retry = promptAssembler.assembleContractRetry(
                    promptContext, exception.failureCode());
            return decode(retry, fallbackModel(retry, cancellation), prepared);
        }
    }

    private ProcessAiModelResult model(ProcessAiPromptBundle prompt,
                                       ProcessAiStreamingMessageWriter partial,
                                       ProcessAiStreamSink sink,
                                       ProcessAiCancellation cancellation) {
        return modelExecutor.parse(prompt.prompt(), delta -> {
            // Model JSON is untrusted and must not be echoed before the contract decoder accepts it.
        }, cancellation);
    }

    private ProcessAiModelResult fallbackModel(ProcessAiPromptBundle prompt,
                                               ProcessAiCancellation cancellation) {
        return modelExecutor.parseFallback(prompt.prompt(), delta -> {
            // Fallback JSON is equally untrusted and remains buffered until validation succeeds.
        }, cancellation);
    }

    private ProcessAiModelExecution decode(ProcessAiPromptBundle prompt,
                                            ProcessAiModelResult model,
                                            ProcessAiPreparedParse prepared) {
        if (resultKindDetector != null && resultKindDetector.detect(model.content())
                == ProcessAiResultKindDetector.ProcessAiResultKind.UNDERSTANDING) {
            var understanding = understandingParser.parse(model.content());
            if (understandingEvidenceSanitizer != null) {
                understanding = understandingEvidenceSanitizer.sanitize(understanding,
                        prepared.redaction().sanitizedText(), prepared.orderContext(),
                        prompt.memoryItemIds());
            }
            requireParseId(prepared.parseId(), understanding.parseId());
            return new ProcessAiModelExecution(prompt, model, null, bindClarificationRevision(
                    understanding, prepared.reservation().parseRevision()));
        }
        ProcessAiExtractionResult verified = evidenceVerifier == null
                ? extractionParser.parse(model.content())
                : evidenceVerifier.verify(extractionParser.parse(model.content()),
                        prepared.redaction().sanitizedText(), prepared.orderContext(),
                        prompt.memoryItemIds());
        ProcessAiExtractionResult normalized = intentNormalizer.normalize(
                verified, prepared.redaction().sanitizedText());
        ProcessAiExtractionResult assigned = sourceAssignmentResolver.resolve(
                normalized, prepared.orderContext(), prepared.redaction().sanitizedText());
        if (assigned == null) assigned = normalized;
        ProcessAiExtractionResult grouped = groupedPiecePlanGuard.resolve(
                assigned, prepared.orderContext());
        ProcessAiExtractionResult extraction = sawRemainderResolver.resolve(
                grouped, prepared.orderContext(), prepared.redaction().sanitizedText());
        requireParseId(prepared.parseId(), extraction.parseId());
        intentValidator.validate(extraction, prepared.orderContext());
        return new ProcessAiModelExecution(prompt, model, extraction);
    }

    private void requireParseId(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new ProcessAiProviderException(
                    "AI_MODEL_PARSE_ID_MISMATCH", false, "AI解析结果请求标识不匹配");
        }
    }

    private com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult bindClarificationRevision(
            com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult understanding,
            int parseRevision) {
        List<ProcessAiClarificationQuestion> questions = understanding.clarificationQuestions().stream()
                .map(question -> new ProcessAiClarificationQuestion(question.questionId(),
                        question.field(), parseRevision, question.question(), question.options(),
                        question.allowUnknown()))
                .toList();
        return new com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult(
                understanding.parseId(), understanding.schemaVersion(), understanding.conclusion(),
                understanding.evidence(), understanding.assumptions(), understanding.risks(), questions,
                understanding.needsClarification());
    }
}
