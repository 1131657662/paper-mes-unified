package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.entity.ReceiveRecord;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/** Produces a stable fingerprint for a receipt retry with the same request id. */
public final class SettleReceiveRequestFingerprint {

    private SettleReceiveRequestFingerprint() {
    }

    public static String of(ReceiveDTO dto) {
        return sha256(canonical(
                dto.getReceiveAmount(), dto.getCashAmount(), dto.getScrapOffsetAmount(),
                dto.getDiscountAmount(), dto.getDiscountReason(), dto.getDiscountApprovalUuid(),
                dto.getScrapWeight(), dto.getPayMethod(), dto.getPayNo(), dto.getReceiveDate(),
                dto.getRemark()));
    }

    /** Rebuilds the legacy payload fingerprint when request_hash was introduced later. */
    public static String of(ReceiveRecord record) {
        return sha256(canonical(
                record.getReceiveAmount(), record.getCashAmount(), record.getScrapOffsetAmount(),
                record.getDiscountAmount(), record.getDiscountReason(), record.getDiscountApprovalUuid(),
                record.getScrapWeight(), record.getPayMethod(), record.getPayNo(), record.getReceiveDate(),
                record.getRemark()));
    }

    /** Compares a pre-hash receipt with the normalized values persisted by the old endpoint. */
    public static boolean matchesLegacy(ReceiveDTO dto, ReceiveRecord record) {
        BigDecimal cash = dto.getCashAmount() != null || dto.getScrapOffsetAmount() != null
                || dto.getDiscountAmount() != null ? moneyValue(dto.getCashAmount()) : moneyValue(dto.getReceiveAmount());
        BigDecimal scrap = moneyValue(dto.getScrapOffsetAmount());
        BigDecimal discount = moneyValue(dto.getDiscountAmount());
        BigDecimal total = cash.add(scrap).add(discount).setScale(2, java.math.RoundingMode.HALF_UP);
        if (!total.equals(moneyValue(record.getReceiveAmount()))
                || !cash.equals(moneyValue(record.getCashAmount()))
                || !scrap.equals(moneyValue(record.getScrapOffsetAmount()))
                || !discount.equals(moneyValue(record.getDiscountAmount()))
                || !weight(dto.getScrapWeight()).equals(weight(record.getScrapWeight()))) {
            return false;
        }
        if (!Objects.equals(text(dto.getDiscountReason()), text(record.getDiscountReason()))
                || !Objects.equals(text(dto.getDiscountApprovalUuid()), text(record.getDiscountApprovalUuid()))
                || !Objects.equals(text(dto.getPayNo()), text(record.getPayNo()))
                || !Objects.equals(text(dto.getRemark()), text(record.getRemark()))) {
            return false;
        }
        if (dto.getPayMethod() != null && !Objects.equals(dto.getPayMethod(), record.getPayMethod())) {
            return false;
        }
        return dto.getReceiveDate() == null || Objects.equals(dto.getReceiveDate(), record.getReceiveDate());
    }

    private static String canonical(Object receiveAmount, Object cashAmount, Object scrapOffsetAmount,
                                    Object discountAmount, Object discountReason, Object approvalUuid,
                                    Object scrapWeight, Object payMethod, Object payNo, Object receiveDate,
                                    Object remark) {
        return String.join("|",
                money((BigDecimal) receiveAmount), money((BigDecimal) cashAmount),
                money((BigDecimal) scrapOffsetAmount), money((BigDecimal) discountAmount),
                text(discountReason), text(approvalUuid), weight((BigDecimal) scrapWeight),
                text(payMethod), text(payNo), date((LocalDateTime) receiveDate), text(remark));
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal moneyValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String weight(BigDecimal value) {
        return value == null ? "0.000" : value.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String date(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
