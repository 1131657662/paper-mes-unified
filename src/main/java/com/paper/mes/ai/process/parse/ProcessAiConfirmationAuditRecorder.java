package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditEntry;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditService;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
class ProcessAiConfirmationAuditRecorder {

    private final ProcessAiCallAuditService auditService;
    private final ProcessAiConfirmationCodec codec;

    void record(ProcessAiConfirmationPreparation preparation,
                ProcessAiConfirmResponse response) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRecord(preparation, response);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeRecord(preparation, response);
            }
        });
    }

    private void safeRecord(ProcessAiConfirmationPreparation preparation,
                             ProcessAiConfirmResponse response) {
        try {
            persist(preparation, response);
        } catch (RuntimeException ex) {
            log.error("Could not persist confirmed AI call audit: conversationId={}, type={}",
                    preparation.load().record().conversationId(), ex.getClass().getSimpleName());
        }
    }

    private void persist(ProcessAiConfirmationPreparation preparation,
                          ProcessAiConfirmResponse response) {
        ProcessAiParseRecord parse = preparation.load().record();
        auditService.record(ProcessAiCallAuditEntry.builder()
                .orderUuid(parse.orderUuid())
                .conversationId(parse.conversationId())
                .parseId(parse.parseId())
                .expectedVersion(parse.expectedVersion())
                .action("CONFIRM")
                .idempotencyKey(preparation.load().applyIdempotencyKey())
                .schemaVersion(parse.schemaVersion())
                .projectMemoryVersion(parse.projectMemoryVersion())
                .projectMemoryChecksum(parse.projectMemoryChecksum())
                .projectMemoryItemIds(codec.readPaths(parse.projectMemoryItemIds()))
                .requestHash(requestHash(preparation, response))
                .resultHash(response.planHash())
                .provider(parse.provider())
                .model(parse.model())
                .route(parse.route())
                .outcome("CONFIRMED")
                .createdBy(preparation.userUuid())
                .build());
    }

    private String requestHash(ProcessAiConfirmationPreparation preparation,
                               ProcessAiConfirmResponse response) {
        ProcessAiParseRecord parse = preparation.load().record();
        return ProcessAiAuditHasher.sha256(
                "PROCESS_CONFIRM", parse.orderUuid(), parse.conversationId(), parse.parseId(),
                Integer.toString(parse.expectedVersion()),
                preparation.load().applyIdempotencyKey(),
                codec.write(preparation.load().acceptedFieldPaths()), response.planHash());
    }
}
