package com.paper.mes.ai.process.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiConversationCleanupSchedulerTest {

    @Test
    void retryTerminalOrderCleanupContinuesAfterOneOrderFails() {
        ProcessAiConversationRepository repository = mock(ProcessAiConversationRepository.class);
        ProcessAiConversationCleanupWorker worker = mock(ProcessAiConversationCleanupWorker.class);
        when(repository.findTerminalOrderUuids(100)).thenReturn(List.of("order-1", "order-2"));
        doThrow(new IllegalStateException("database unavailable")).when(worker).close("order-1");
        ProcessAiConversationCleanupScheduler scheduler =
                new ProcessAiConversationCleanupScheduler(repository, worker);

        scheduler.retryTerminalOrderCleanup();

        verify(worker).close("order-1");
        verify(worker).close("order-2");
    }
}
