package com.paper.mes.processorder.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Produces the server-owned payload digest for a reissue request. */
public final class ProcessOrderReissueFingerprint {

    private ProcessOrderReissueFingerprint() {
    }

    public static String of(String orderUuid, Integer expectedVersion, String reason) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, orderUuid);
        append(canonical, expectedVersion);
        append(canonical, reason);
        return sha256(canonical.toString());
    }

    private static void append(StringBuilder canonical, Object value) {
        String text = value == null ? "" : value.toString().trim();
        canonical.append(text.length()).append(':').append(text).append('|');
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
