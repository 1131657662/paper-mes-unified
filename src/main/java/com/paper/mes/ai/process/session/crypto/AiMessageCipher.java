package com.paper.mes.ai.process.session.crypto;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AiMessageCipher {

    private static final String FORMAT_PREFIX = "v1:";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final AiProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public AiMessageCipher(AiProperties properties) {
        this(properties, new SecureRandom());
    }

    AiMessageCipher(AiProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public String encrypt(AiMessageCryptoContext context, String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, context, iv);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return FORMAT_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("AI_MESSAGE_ENCRYPT_FAILED", "AI会话消息加密失败");
        }
    }

    public String decrypt(AiMessageCryptoContext context, String ciphertext) {
        try {
            byte[] payload = decodePayload(ciphertext);
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, context, iv);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("AI_MESSAGE_DECRYPT_FAILED", "AI会话消息无法解密");
        }
    }

    public String hash(String plaintext) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public boolean configured() {
        try {
            key();
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private Cipher cipher(int mode, AiMessageCryptoContext context, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(aad(context));
        return cipher;
    }

    private byte[] key() {
        try {
            byte[] key = Base64.getDecoder().decode(properties.getMessageEncryptionKey());
            if (key.length == KEY_BYTES) return key;
        } catch (IllegalArgumentException ignored) {
            // Converted to a stable fail-closed business error below.
        }
        throw unavailable("AI_MESSAGE_KEY_UNAVAILABLE", "AI会话加密密钥未正确配置");
    }

    private byte[] decodePayload(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(FORMAT_PREFIX)) {
            throw unavailable("AI_MESSAGE_DECRYPT_FAILED", "AI会话消息密文格式无效");
        }
        byte[] payload = Base64.getDecoder().decode(ciphertext.substring(FORMAT_PREFIX.length()));
        if (payload.length <= IV_BYTES) {
            throw unavailable("AI_MESSAGE_DECRYPT_FAILED", "AI会话消息密文格式无效");
        }
        return payload;
    }

    private byte[] aad(AiMessageCryptoContext context) {
        String value = context.conversationId() + "\u0000" + context.sequenceNo()
                + "\u0000" + context.role();
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private BusinessException unavailable(String code, String message) {
        return new BusinessException(ResultCode.SERVICE_UNAVAILABLE, code, message);
    }
}
