package com.paper.mes.report.subscription.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class ReportSubscriptionRequestId {

    private ReportSubscriptionRequestId() {
    }

    public static String generate(String subscriptionUuid, LocalDateTime scheduledFor, String recipientUuid) {
        String source = subscriptionUuid + "|" + scheduledFor.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "|" + recipientUuid;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
