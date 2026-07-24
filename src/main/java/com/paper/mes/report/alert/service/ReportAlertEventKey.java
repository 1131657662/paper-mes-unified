package com.paper.mes.report.alert.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

public final class ReportAlertEventKey {
    private ReportAlertEventKey() {
    }

    public static String generate(String ruleUuid, String releaseUuid, LocalDate periodStart,
                                  LocalDate periodEnd, String dimensionHash) {
        return sha256(String.join("|", ruleUuid, releaseUuid, periodStart.toString(),
                periodEnd.toString(), dimensionHash));
    }

    public static String dimensionHash(String canonicalDimensions) {
        return sha256(canonicalDimensions);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
