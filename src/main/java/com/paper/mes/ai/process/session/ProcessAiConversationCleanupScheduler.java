package com.paper.mes.ai.process.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class ProcessAiConversationCleanupScheduler {

    private static final int BATCH_SIZE = 100;

    private final ProcessAiConversationRepository conversationRepository;
    private final ProcessAiConversationCleanupWorker cleanupWorker;

    @Scheduled(
            initialDelayString = "#{@aiProperties.conversationCleanupDelayMs}",
            fixedDelayString = "#{@aiProperties.conversationCleanupDelayMs}")
    void retryTerminalOrderCleanup() {
        List<String> orderUuids = terminalOrderUuids();
        for (String orderUuid : orderUuids) cleanup(orderUuid);
    }

    private List<String> terminalOrderUuids() {
        try {
            return conversationRepository.findTerminalOrderUuids(BATCH_SIZE);
        } catch (RuntimeException exception) {
            log.error("AI conversation cleanup compensation scan failed: type={}",
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private void cleanup(String orderUuid) {
        try {
            cleanupWorker.close(orderUuid);
        } catch (RuntimeException exception) {
            log.error("AI conversation cleanup compensation failed: orderUuid={}, type={}",
                    orderUuid, exception.getClass().getSimpleName());
        }
    }
}
