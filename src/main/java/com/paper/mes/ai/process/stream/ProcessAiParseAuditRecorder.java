package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditEntry;
import com.paper.mes.ai.process.audit.ProcessAiCallAuditService;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
class ProcessAiParseAuditRecorder {

    private final ProcessAiCallAuditService auditService;
    private final AiProperties properties;

    void success(ProcessAiParseAuditSuccess success) {
        ProcessAiPreparedParse prepared = success.prepared();
        auditService.record(base(prepared)
                .projectMemoryItemIds(success.execution().promptBundle().memoryItemIds())
                .resultHash(success.parseRecord().resultHash())
                .provider(success.execution().modelResult().provider())
                .model(success.execution().modelResult().model())
                .route(success.execution().modelResult().route())
                .outcome(success.outcome())
                .latencyMs(latencyMs(prepared))
                .inputTokens(success.execution().modelResult().inputTokens())
                .outputTokens(success.execution().modelResult().outputTokens())
                .build());
    }

    void successAfterCommit(ProcessAiParseAuditSuccess success) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeSuccess(success);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeSuccess(success);
            }
        });
    }

    private void safeSuccess(ProcessAiParseAuditSuccess success) {
        try {
            success(success);
        } catch (RuntimeException ex) {
            log.error("Could not persist successful AI call audit: conversationId={}, type={}",
                    success.prepared().request().conversationId(), ex.getClass().getSimpleName());
        }
    }

    void failure(ProcessAiPreparedParse prepared, String failureCode) {
        auditService.record(base(prepared)
                .projectMemoryItemIds(List.of())
                .model(configuredModel())
                .outcome("AI_REQUEST_CANCELLED".equals(failureCode) ? "CANCELLED" : "FAILED")
                .failureCode(failureCode)
                .latencyMs(latencyMs(prepared))
                .build());
    }

    private ProcessAiCallAuditEntry.ProcessAiCallAuditEntryBuilder base(
            ProcessAiPreparedParse prepared) {
        return ProcessAiCallAuditEntry.builder()
                .orderUuid(prepared.orderUuid())
                .conversationId(prepared.request().conversationId())
                .parseId(prepared.parseId())
                .expectedVersion(prepared.request().expectedVersion())
                .action(prepared.request().action())
                .idempotencyKey(prepared.request().idempotencyKey())
                .schemaVersion("1.0")
                .projectMemoryVersion(prepared.memory().docVersion())
                .projectMemoryChecksum(prepared.memory().checksum())
                .requestHash(requestHash(prepared))
                .provider(properties.providerMode().name())
                .route("PRO")
                .createdBy(currentUserUuid());
    }

    private String requestHash(ProcessAiPreparedParse prepared) {
        return ProcessAiAuditHasher.sha256(
                "PROCESS_PARSE", prepared.orderUuid(), prepared.request().conversationId(),
                Integer.toString(prepared.request().expectedVersion()),
                prepared.request().action(), prepared.request().idempotencyKey(),
                prepared.redaction().sanitizedText(), prepared.memory().checksum());
    }

    private String configuredModel() {
        if (properties.providerMode() == AiProvider.DEEPSEEK) {
            return properties.getDeepseekModelPro();
        }
        if (properties.providerMode() == AiProvider.ZHIPU) {
            return properties.getZhipuModel();
        }
        return "unconfigured";
    }

    private int latencyMs(ProcessAiPreparedParse prepared) {
        long nanos = Math.max(0L, System.nanoTime() - prepared.startedAtNanos());
        return (int) Math.min(Integer.MAX_VALUE, TimeUnit.NANOSECONDS.toMillis(nanos));
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        return user == null ? null : user.getUuid();
    }
}
