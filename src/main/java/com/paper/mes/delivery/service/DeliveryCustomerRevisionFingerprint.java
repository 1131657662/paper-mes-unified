package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.DeliveryCustomerRevisionRequestDTO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecItemDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class DeliveryCustomerRevisionFingerprint {

    private DeliveryCustomerRevisionFingerprint() {}

    public static String of(DeliveryCustomerRevisionRequestDTO request) {
        StringBuilder value = new StringBuilder();
        append(value, request.getExpectedDeliveryVersion());
        append(value, request.getReason());
        request.getItems().stream()
                .sorted(Comparator.comparing(DeliveryCustomerSpecItemDTO::getDeliveryDetailUuid))
                .forEach(item -> appendItem(value, item));
        return sha256(value.toString());
    }

    private static void appendItem(StringBuilder value, DeliveryCustomerSpecItemDTO item) {
        append(value, item.getDeliveryDetailUuid());
        append(value, item.getExpectedDetailVersion());
        append(value, item.getCustomerPaperName());
        append(value, item.getCustomerGramWeight());
        append(value, item.getCustomerFinishWidth());
        append(value, number(item.getCustomerDisplayWeight()));
        append(value, item.getCalculationMode());
        append(value, number(item.getWeightOperand()));
        append(value, item.getFormulaExpression());
        appendVariables(value, item.getFormulaVariables());
        append(value, item.getRoundingScale());
        append(value, item.getRoundingMode());
        append(value, item.getZeroPolicy());
        append(value, item.getCustomerRemark());
    }

    private static void appendVariables(StringBuilder value, Map<String, BigDecimal> variables) {
        if (variables == null) { append(value, null); return; }
        new TreeMap<>(variables).forEach((key, variable) -> {
            append(value, key);
            append(value, number(variable));
        });
    }

    private static void append(StringBuilder value, Object part) {
        String text = part == null ? "" : part.toString().trim();
        value.append(text.length()).append(':').append(text).append('|');
    }

    private static String number(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
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
