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
            String intentJson = objectMapper.writeValueAsString(command.extraction());
            String itemIds = objectMapper.writeValueAsString(command.memoryItemIds());
            ProcessAiParseRecord row = row(command, itemIds, intentJson);
            if (repository.insert(row) != 1) {
                throw new IllegalStateException("AI parse insert did not affect one row");
            }
            return row;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.ERROR,
                    "AI_PARSE_SERIALIZATION_FAILED", "AI解析候选保存失败");
        }
    }

    private ProcessAiParseRecord row(ProcessAiParseStoreCommand command, String itemIds,
                                     String intentJson) {
        String storedIntent = intentCipher.encrypt(
                command.conversationId(), command.parseRevision(), intentJson);
        return new ProcessAiParseRecord(
                UUID.randomUUID().toString(), command.orderUuid(), command.conversationId(),
                command.extraction().parseId(), command.parseRevision(), command.memoryGeneration(),
                command.requestIdempotencyKey(), command.expectedVersion(), command.status(),
                command.modelResult().provider(), command.modelResult().model(),
                command.modelResult().route(), "1.0",
                command.memory().docVersion(), command.memory().checksum(), itemIds,
                storedIntent, sha256(intentJson), ProcessAiParseConfirmation.empty(),
                LocalDateTime.now());
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
}
