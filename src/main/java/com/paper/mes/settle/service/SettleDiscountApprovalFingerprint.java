package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SettleDiscountApprovalFingerprint {
    private SettleDiscountApprovalFingerprint() {
    }

    public static String of(String settleUuid, SettleDiscountApprovalRequestDTO dto) {
        String canonical = String.join("|", settleUuid, money(dto.getCashAmount()),
                money(dto.getScrapOffsetAmount()), money(dto.getDiscountAmount()),
                money(dto.getUnreceivedSnapshot()), dto.getReason().trim());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
