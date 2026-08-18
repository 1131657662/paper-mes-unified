package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProcessAiParseAsyncRunnerTest {

    @Test
    void submit_whenQueueRejects_marksParseFailedAndRethrows() {
        ProcessAiParseExecutionService executionService = mock(ProcessAiParseExecutionService.class);
        ProcessAiParseFailureService failureService = mock(ProcessAiParseFailureService.class);
        ProcessAiTaskExecutor taskExecutor = mock(ProcessAiTaskExecutor.class);
        ProcessAiPreparedParse prepared = mock(ProcessAiPreparedParse.class);
        ProcessAiSseSink sink = mock(ProcessAiSseSink.class);
        BusinessException busy = new BusinessException(
                ResultCode.TOO_MANY_REQUESTS, "AI_BUSY", "AI assistant is busy");
        doThrow(busy).when(taskExecutor).submit(org.mockito.ArgumentMatchers.any());
        ProcessAiParseAsyncRunner runner = new ProcessAiParseAsyncRunner(
                executionService, failureService, taskExecutor);

        assertThatThrownBy(() -> runner.submit(
                prepared, sink, new ProcessAiCancellation())).isSameAs(busy);

        verify(failureService).failBeforeStart(prepared, "AI_BUSY");
    }
}
