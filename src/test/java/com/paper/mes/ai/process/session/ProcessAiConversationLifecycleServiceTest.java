package com.paper.mes.ai.process.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.paper.mes.ai.process.stream.ProcessAiSingleFlightRegistry;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiConversationLifecycleServiceTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void closeAfterCommitDefersCleanupUntilTheBusinessTransactionCommits() {
        ProcessAiConversationCleanupWorker worker = mock(ProcessAiConversationCleanupWorker.class);
        ProcessAiConversationLifecycleService lifecycle = new ProcessAiConversationLifecycleService(worker);
        TransactionSynchronizationManager.initSynchronization();

        lifecycle.closeAfterCommit("order-1");
        verify(worker, never()).close("order-1");
        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(worker).close("order-1");
    }

    @Test
    void closeAfterCommitDoesNotCleanMessagesWhenTheTransactionRollsBack() {
        ProcessAiConversationCleanupWorker worker = mock(ProcessAiConversationCleanupWorker.class);
        ProcessAiConversationLifecycleService lifecycle = new ProcessAiConversationLifecycleService(worker);
        TransactionSynchronizationManager.initSynchronization();

        lifecycle.closeAfterCommit("order-1");
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(worker, never()).close("order-1");
    }

    @Test
    void closeAfterCommitCleansImmediatelyOutsideATransaction() {
        ProcessAiConversationCleanupWorker worker = mock(ProcessAiConversationCleanupWorker.class);

        new ProcessAiConversationLifecycleService(worker).closeAfterCommit("order-1");

        verify(worker).close("order-1");
    }

    @Test
    void cleanupWorkerDeletesMessageBodiesBeforeClosingTheConversation() {
        ProcessAiConversationRepository conversations = mock(ProcessAiConversationRepository.class);
        ProcessAiMessageRepository messages = mock(ProcessAiMessageRepository.class);
        ProcessAiSingleFlightRegistry flights = mock(ProcessAiSingleFlightRegistry.class);
        when(conversations.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation()));

        new ProcessAiConversationCleanupWorker(conversations, messages, flights).close("order-1");

        var ordered = org.mockito.Mockito.inOrder(messages, conversations);
        ordered.verify(messages).deleteByConversation("conversation-1");
        ordered.verify(conversations).close("conversation-1");
    }

    @Test
    void cleanupWorkerLeavesAnInFlightConversationUntouched() {
        ProcessAiConversationRepository conversations = mock(ProcessAiConversationRepository.class);
        ProcessAiMessageRepository messages = mock(ProcessAiMessageRepository.class);
        ProcessAiSingleFlightRegistry flights = mock(ProcessAiSingleFlightRegistry.class);
        when(conversations.findByOrderForUpdate("order-1"))
                .thenReturn(Optional.of(conversation()));
        when(flights.isConversationInFlight("conversation-1")).thenReturn(true);

        new ProcessAiConversationCleanupWorker(conversations, messages, flights).close("order-1");

        verify(messages, never()).deleteByConversation("conversation-1");
        verify(conversations, never()).close("conversation-1");
    }

    private ProcessAiConversationRow conversation() {
        return new ProcessAiConversationRow("row-1", "conversation-1", "order-1",
                "user-1", 4, 7, "1.0.0", 1, "OPEN");
    }
}
