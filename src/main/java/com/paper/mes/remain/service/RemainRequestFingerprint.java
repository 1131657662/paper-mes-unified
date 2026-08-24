package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainRegistrationCreateDTO;
import com.paper.mes.remain.dto.RemainSaleCreateDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Collectors;

public final class RemainRequestFingerprint {

    private RemainRequestFingerprint() {
    }

    public static String registration(RemainRegistrationCreateDTO request) {
        String lines = request.getLines().stream()
                .sorted(Comparator.comparing(line -> line.getSourceFinishRollUuid()))
                .map(line -> line.getSourceFinishRollUuid() + "=" + line.getTransferredSystemWeight())
                .collect(Collectors.joining(";"));
        String payload = String.join("|", request.getOrderUuid(), request.getConfirmationName(),
                request.getConfirmationChannel(), request.getConfirmationAt().toString(),
                request.getConfirmationEvidence(), lines);
        return sha256(payload);
    }

    public static String ledgerRequest(String requestId, String lineUuid, String eventType) {
        return sha256(requestId + "|" + lineUuid + "|" + eventType);
    }

    public static String sale(RemainSaleCreateDTO request) {
        String lines = request.getLines().stream()
                .sorted(Comparator.comparing(line -> line.getLotUuid()))
                .map(line -> line.getLotUuid() + "=" + line.getSystemWeight())
                .collect(Collectors.joining(";"));
        String payload = String.join("|", request.getProcessDate().toString(),
                String.valueOf(request.getWarehouseUuid()), request.getPricingMode(),
                String.valueOf(request.getActualWeight()), String.valueOf(request.getUnitPrice()),
                String.valueOf(request.getTotalAmount()), String.valueOf(request.getReceivedAmount()), lines);
        return sha256(payload);
    }

    public static String adjustmentRequest(String sourceRequestId) {
        return sha256("REMAIN_ADJUSTMENT|" + sourceRequestId);
    }

    public static String application(String registrationUuid, String settleUuid,
                                     BigDecimal amount, BigDecimal weight) {
        return sha256("REMAIN_APPLICATION|" + registrationUuid + "|" + settleUuid + "|"
                + amount.toPlainString() + "|" + weight.toPlainString());
    }

    public static String adjustment(String registrationUuid, String sourceSettleUuid,
                                    BigDecimal amount, BigDecimal weight) {
        return sha256("REMAIN_ADJUSTMENT|" + registrationUuid + "|" + sourceSettleUuid + "|"
                + amount.toPlainString() + "|" + weight.toPlainString());
    }

    public static String refund(String adjustmentUuid, BigDecimal amount, BigDecimal weight) {
        return sha256("REMAIN_REFUND|" + adjustmentUuid + "|" + amount.toPlainString() + "|"
                + weight.toPlainString());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256不可用", exception);
        }
    }
}
