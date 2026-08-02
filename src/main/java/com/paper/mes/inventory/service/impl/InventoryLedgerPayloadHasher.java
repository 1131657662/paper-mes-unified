package com.paper.mes.inventory.service.impl;

import com.paper.mes.inventory.dto.InventoryLedgerCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class InventoryLedgerPayloadHasher {

    private InventoryLedgerPayloadHasher() {
    }

    static String hash(InventoryLedgerCommand command) {
        String payload = String.join("|", command.getFinishRollUuid(), command.getEventType().name(),
                command.getSourceBusinessType(), command.getSourceBusinessUuid(),
                command.getQuantityDelta().toPlainString(), command.getWeightDelta().toPlainString(),
                command.getReservedQuantityDelta().toPlainString(), command.getReservedWeightDelta().toPlainString(),
                empty(command.getReason()), empty(command.getOperatorUuid()), command.getOperatorName(),
                command.getOccurredAt().toString(), command.getIdempotencyKey());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }
}
