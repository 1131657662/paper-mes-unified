package com.paper.mes.ai.process.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessAiConversationLifecycleService {

    private final ProcessAiConversationCleanupWorker cleanupWorker;

    public void closeAfterCommit(String orderUuid) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanup(orderUuid);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cleanup(orderUuid);
            }
        });
    }

    private void cleanup(String orderUuid) {
        try {
            cleanupWorker.close(orderUuid);
        } catch (RuntimeException ex) {
            log.error("AI conversation cleanup failed after order terminal transition: orderUuid={}, type={}",
                    orderUuid, ex.getClass().getSimpleName());
        }
    }
}
