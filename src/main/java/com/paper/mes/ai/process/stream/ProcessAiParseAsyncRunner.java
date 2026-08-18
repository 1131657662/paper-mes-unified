package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
            failureService.fail(prepared, failure.code());
            sink.error(failure.code(), failure.message(), failure.retryable());
            sink.done();
        } finally {
            AuthContextHolder.clear();
        }
    }
}
