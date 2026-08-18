package com.paper.mes.ai.memory.candidate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
class ProjectMemoryCandidateCaptureListener {

    private final ProjectMemoryLearningOutboxService outboxService;

    /**
     * Enqueue in the confirmation transaction so a database failure cannot
     * acknowledge the order while losing the learning event.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void afterConfirmed(ProjectMemoryCandidateConfirmedEvent event) {
        outboxService.enqueueConfirmed(event);
    }
}
