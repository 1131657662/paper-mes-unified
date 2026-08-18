package com.paper.mes.ai.process.session.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiStructuredResultCipher {

    private static final String FORMAT = "aes-gcm-v1";

    private final AiMessageCipher cipher;
    private final ObjectMapper objectMapper;

    public String encrypt(AiMessageCryptoContext messageContext, String plaintext) {
        if (plaintext == null) return null;
        try {
            String ciphertext = cipher.encrypt(structuredContext(messageContext), plaintext);
            return objectMapper.writeValueAsString(Map.of(
                    "format", FORMAT,
                    "ciphertext", ciphertext));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("AI_STRUCTURED_RESULT_ENCRYPT_FAILED",
                    "AI结构化结果加密失败");
        }
    }

    public String decrypt(AiMessageCryptoContext messageContext, String storedValue) {
        if (storedValue == null) return null;
        try {
            JsonNode root = objectMapper.readTree(storedValue);
            if (!isEncryptedEnvelope(root)) return storedValue;
            return cipher.decrypt(structuredContext(messageContext),
                    root.path("ciphertext").asText());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("AI_STRUCTURED_RESULT_DECRYPT_FAILED",
                    "AI结构化结果无法解密");
        }
    }

    private boolean isEncryptedEnvelope(JsonNode root) {
        return root != null && root.isObject()
                && FORMAT.equals(root.path("format").asText())
                && root.path("ciphertext").isTextual();
    }

    private AiMessageCryptoContext structuredContext(AiMessageCryptoContext context) {
        return new AiMessageCryptoContext(
                context.conversationId(), context.sequenceNo(),
                "STRUCTURED:" + context.role());
    }

    private BusinessException unavailable(String code, String message) {
        return new BusinessException(ResultCode.SERVICE_UNAVAILABLE, code, message);
    }
}
