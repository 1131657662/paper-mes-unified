package com.paper.mes.ai.process.credential;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AiProviderSecretCipher {

    private static final String PREFIX = "v1:";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final AiProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public AiProviderSecretCipher(AiProperties properties) {
        this(properties, new SecureRandom());
    }

    AiProviderSecretCipher(AiProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    String encrypt(AiProvider provider, String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            byte[] encrypted = cipher(Cipher.ENCRYPT_MODE, provider, iv)
                    .doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("AI_PROVIDER_KEY_ENCRYPT_FAILED");
        }
    }

    String decrypt(AiProvider provider, String ciphertext) {
        try {
            byte[] payload = decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            byte[] plaintext = cipher(Cipher.DECRYPT_MODE, provider, iv).doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("AI_PROVIDER_KEY_DECRYPT_FAILED");
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

    private Cipher cipher(int mode, AiProvider provider, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(("paper-mes-ai-provider-secret\0" + provider.name())
                .getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private byte[] key() {
        try {
            byte[] key = Base64.getDecoder().decode(properties.getConfigMasterKey());
            if (key.length == KEY_BYTES) return key;
        } catch (IllegalArgumentException ignored) {
            // Converted to one stable fail-closed error.
        }
        throw unavailable("AI_CONFIG_MASTER_KEY_UNAVAILABLE");
    }

    private byte[] decode(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            throw unavailable("AI_PROVIDER_KEY_DECRYPT_FAILED");
        }
        byte[] payload = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
        if (payload.length <= IV_BYTES) throw unavailable("AI_PROVIDER_KEY_DECRYPT_FAILED");
        return payload;
    }

    private BusinessException unavailable(String code) {
        return new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                code, "AI provider credential encryption is unavailable");
    }
}
