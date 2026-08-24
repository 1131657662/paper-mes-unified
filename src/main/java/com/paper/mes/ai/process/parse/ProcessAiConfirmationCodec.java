package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiCorrection;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import com.paper.mes.ai.process.session.crypto.AiStructuredResultCipher;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
class ProcessAiConfirmationCodec {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private static final TypeReference<List<ProcessAiCorrection>> CORRECTION_LIST =
            new TypeReference<>() { };

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AiStructuredResultCipher structuredResultCipher;
    private final ProcessAiIntentCipher intentCipher;

    ProcessAiExtractionResult readExtraction(ProcessAiParseRecord record) {
        String plaintext = intentCipher.decrypt(
                record.conversationId(), record.parseRevision(), record.intentJson());
        ProcessAiExtractionResult result = read(plaintext, ProcessAiExtractionResult.class);
        Set<ConstraintViolation<ProcessAiExtractionResult>> violations = validator.validate(result);
        if (!violations.isEmpty()) throw corrupted();
        return result;
    }

    ProcessAiConfirmResponse readResponse(String json, String conversationId, int parseRevision) {
        String plaintext = structuredResultCipher.decrypt(
                responseContext(conversationId, parseRevision), json);
        return read(plaintext, ProcessAiConfirmResponse.class);
    }

    String writeResponse(ProcessAiConfirmResponse response) {
        String plaintext = write(response);
        return structuredResultCipher.encrypt(
                responseContext(response.conversationId(), response.parseRevision()), plaintext);
    }

    List<String> readPaths(String json) {
        try {
            return List.copyOf(objectMapper.readValue(json, STRING_LIST));
        } catch (Exception ex) {
            throw corrupted();
        }
    }

    List<ProcessAiCorrection> readCorrections(ProcessAiParseRecord record) {
        if (record.correctionsJson() == null) return List.of();
        try {
            String plaintext = intentCipher.decrypt(record.conversationId(),
                    record.parseRevision(), record.correctionsJson());
            List<ProcessAiCorrection> corrections = objectMapper.readValue(
                    plaintext, CORRECTION_LIST);
            Set<ConstraintViolation<ProcessAiCorrection>> violations = corrections.stream()
                    .flatMap(item -> validator.validate(item).stream())
                    .collect(java.util.stream.Collectors.toSet());
            if (!violations.isEmpty()) throw corrupted();
            return List.copyOf(corrections);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw corrupted();
        }
    }

    String correctionsHash(ProcessAiParseRecord record) {
        if (record.correctionsJson() == null) return null;
        String plaintext = intentCipher.decrypt(record.conversationId(),
                record.parseRevision(), record.correctionsJson());
        return sha256(plaintext);
    }

    String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.ERROR,
                    "AI_CONFIRM_SERIALIZATION_FAILED", "AI confirmation could not be stored");
        }
    }

    String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw corrupted();
        }
    }

    private BusinessException corrupted() {
        return new BusinessException(ResultCode.ERROR,
                "AI_PARSE_STORED_DATA_INVALID", "Stored AI parse data is invalid");
    }

    private AiMessageCryptoContext responseContext(String conversationId, int parseRevision) {
        return new AiMessageCryptoContext(conversationId, parseRevision, "CONFIRMATION");
    }
}
