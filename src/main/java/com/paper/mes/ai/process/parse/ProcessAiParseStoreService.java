package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessAiParseStoreService {

    private final ProcessAiParseRepository repository;
    private final ObjectMapper objectMapper;
    private final ProcessAiIntentCipher intentCipher;

    @Transactional(readOnly = true)
    public Optional<ProcessAiParseRecord> findReplay(String conversationId, String requestKey) {
        return repository.findByRequestKey(conversationId, requestKey);
    }

    @Transactional
    public ProcessAiParseRecord store(ProcessAiParseStoreCommand command) {
        try {
            String intentJson = command.extraction() == null
                    ? null : objectMapper.writeValueAsString(command.extraction());
            String understandingJson = command.understanding() == null
                    ? null : objectMapper.writeValueAsString(command.understanding());
            String itemIds = objectMapper.writeValueAsString(command.memoryItemIds());
            ProcessAiParseRecord row = row(command, itemIds, intentJson, understandingJson);
            if (repository.insert(row) != 1) {
                throw new IllegalStateException("AI parse insert did not affect one row");
            }
            return row;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.ERROR,
                    "AI_PARSE_SERIALIZATION_FAILED", "AI解析候选保存失败");
        }
    }

    @Transactional
    public ProcessAiParseRecord storeFailure(ProcessAiFailureStoreCommand failure) {
        String traceId = failure.failureTraceId() == null
                ? UUID.randomUUID().toString() : failure.failureTraceId();
        ProcessAiParseStoreCommand command = new ProcessAiParseStoreCommand(
                failure.orderUuid(), failure.conversationId(), failure.expectedVersion(),
                failure.parseRevision(), failure.memoryGeneration(), failure.requestIdempotencyKey(),
                "INTERRUPTED", failure.memory(), java.util.List.of(),
                new com.paper.mes.ai.process.model.ProcessAiModelResult(
                        null, failure.model(), failure.provider(), failure.route(), null, null),
                null, null, "FAILED", "FAILURE", 2, null, null, null, null, null, null,
                failure.failureCode(), traceId, null, null, failure.parseId());
        return store(command);
    }

    private ProcessAiParseRecord row(ProcessAiParseStoreCommand command, String itemIds,
                                     String intentJson, String understandingJson) {
        String storedIntent = encrypt(command, intentJson);
        String storedUnderstanding = encrypt(command, understandingJson);
        String storedQuestion = encrypt(command, command.questionJson());
        return new ProcessAiParseRecord(
                UUID.randomUUID().toString(), command.orderUuid(), command.conversationId(),
                command.parseId(), command.parseRevision(), command.memoryGeneration(),
                command.requestIdempotencyKey(), command.expectedVersion(), command.status(),
                command.modelResult().provider(), command.modelResult().model(),
                command.modelResult().route(), "UNDERSTANDING".equals(command.resultKind())
                        || "FAILURE".equals(command.resultKind()) ? "2.0" : "1.0",
                command.memory().docVersion(), command.memory().checksum(), itemIds,
                storedIntent, resultHash(command, intentJson, understandingJson),
                ProcessAiParseConfirmation.empty(), LocalDateTime.now(),
                command.dialogueState(), command.resultKind(),
                command.workflowVersion(), storedUnderstanding, storedQuestion,
                command.correctionsJson(), command.inputHash(), command.contextHash(),
                command.previewHash(), command.failureCode(), command.failureTraceId(),
                command.requiredDefaultIds(), command.acknowledgedDefaultIds());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String sha256Nullable(String value) {
        return value == null ? null : sha256(value);
    }

    private String resultHash(ProcessAiParseStoreCommand command, String intentJson,
                              String understandingJson) {
        if (intentJson != null) return sha256(intentJson);
        if (understandingJson != null) return sha256(understandingJson);
        return sha256(String.valueOf(command.failureCode()) + ":" + command.failureTraceId());
    }

    private String encrypt(ProcessAiParseStoreCommand command, String value) {
        return value == null ? null : intentCipher.encrypt(
                command.conversationId(), command.parseRevision(), value);
    }
}
