package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class ProcessAiParseAsyncRunner {

    private final ProcessAiParseExecutionService executionService;
    private final ProcessAiParseFailureService failureService;
    private final ProcessAiTaskExecutor taskExecutor;

    void submit(ProcessAiPreparedParse prepared, ProcessAiStreamSink sink,
                ProcessAiCancellation cancellation) {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        try {
            var task = taskExecutor.submit(() -> run(prepared, sink, user, cancellation));
            cancellation.onCancel(() -> task.cancel(true));
        } catch (RuntimeException ex) {
            failureService.failBeforeStart(prepared, ProcessAiFailure.classify(ex).code());
            throw ex;
        }
    }

    private void run(ProcessAiPreparedParse prepared, ProcessAiStreamSink sink, CurrentUser user,
                     ProcessAiCancellation cancellation) {
        AuthContextHolder.setCurrentUser(user);
        try {
            cancellation.throwIfCancelled();
            executionService.execute(prepared, sink, cancellation);
            sink.done();
        } catch (RuntimeException ex) {
            ProcessAiFailure failure = ProcessAiFailure.classify(ex);
            log.error("AI parse execution failed: conversationId={}, parseId={}, failureCode={}, exceptionType={}, causeType={}, causeMessage={}",
                    prepared.request().conversationId(), prepared.parseId(), failure.code(),
                    ex.getClass().getSimpleName(), causeType(ex), causeMessage(ex));
            failureService.fail(prepared, failure.code());
            sink.error(failure.code(), failure.message(), failure.retryable());
            sink.done();
        } finally {
            AuthContextHolder.clear();
        }
    }

    private String causeType(RuntimeException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getClass().getSimpleName();
    }

    private String causeMessage(RuntimeException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return "<none>";
        message = message.replaceAll("[\\r\\n]+", " ");
        return message.length() > 240 ? message.substring(0, 240) : message;
    }
}
