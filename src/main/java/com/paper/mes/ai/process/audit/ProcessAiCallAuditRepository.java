package com.paper.mes.ai.process.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
class ProcessAiCallAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    int insert(ProcessAiCallAuditEntry entry, String memoryItemIdsJson) {
        return jdbcTemplate.update("""
                INSERT INTO sys_ai_call_audit
                  (uuid, order_uuid, conversation_id, parse_id, expected_version, action,
                   idempotency_key, schema_version, project_memory_version,
                   project_memory_checksum, project_memory_item_ids, request_hash, result_hash,
                   provider, model, route, outcome, failure_code, latency_ms, input_tokens,
                   output_tokens, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), entry.orderUuid(), entry.conversationId(),
                entry.parseId(), entry.expectedVersion(), entry.action(), entry.idempotencyKey(),
                entry.schemaVersion(), entry.projectMemoryVersion(),
                entry.projectMemoryChecksum(), memoryItemIdsJson, entry.requestHash(),
                entry.resultHash(), entry.provider(), entry.model(), entry.route(), entry.outcome(),
                entry.failureCode(), entry.latencyMs(), entry.inputTokens(), entry.outputTokens(),
                entry.createdBy());
    }
}
