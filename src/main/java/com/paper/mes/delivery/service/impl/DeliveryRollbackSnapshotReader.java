package com.paper.mes.delivery.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryRollbackSnapshotVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reads the previous confirm snapshot stored inside a delivery rollback snapshot.
 */
final class DeliveryRollbackSnapshotReader {

    private DeliveryRollbackSnapshotReader() {
    }

    static DeliveryRollbackSnapshotVO read(String snapDelivery, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(snapDelivery)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(snapDelivery);
            if (!isSnapshotType(root, "delivery_rollback")) {
                return null;
            }
            JsonNode previous = firstExisting(root, "previous_confirm_snapshot", "previousConfirmSnapshot");
            if (previous == null || !previous.isObject()) {
                return null;
            }
            List<DeliveryDetailItemVO> details = readPreviousDetails(previous, objectMapper);
            DeliveryRollbackSnapshotVO snapshot = new DeliveryRollbackSnapshotVO();
            snapshot.setDeliveryNo(text(previous, "delivery_no", "deliveryNo"));
            snapshot.setRollbackReason(text(root, "rollback_reason", "rollbackReason"));
            snapshot.setRollbackOperator(text(root, "rollback_operator", "rollbackOperator"));
            snapshot.setRollbackTime(dateTime(root, "rollback_time", "rollbackTime"));
            snapshot.setSignUser(text(previous, "sign_user", "signUser"));
            snapshot.setSignTime(dateTime(previous, "sign_time", "signTime"));
            snapshot.setTotalCount(integer(previous, "total_count", "totalCount"));
            snapshot.setTotalWeight(decimal(previous, "total_weight", "totalWeight"));
            snapshot.setDetails(details == null ? List.of() : details);
            return snapshot;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<DeliveryDetailItemVO> readPreviousDetails(JsonNode previous, ObjectMapper objectMapper) {
        try {
            return DeliverySnapshotItemReader.read(objectMapper.writeValueAsString(previous), objectMapper);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isSnapshotType(JsonNode root, String type) {
        JsonNode node = firstExisting(root, "snapshot_type", "snapshotType");
        return node != null && type.equals(node.asText());
    }

    private static JsonNode firstExisting(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private static String text(JsonNode root, String snakeName, String camelName) {
        JsonNode node = firstExisting(root, snakeName, camelName);
        return node == null ? null : node.asText();
    }

    private static Integer integer(JsonNode root, String snakeName, String camelName) {
        JsonNode node = firstExisting(root, snakeName, camelName);
        if (node == null) {
            return null;
        }
        try {
            return node.isNumber() ? node.asInt() : Integer.valueOf(node.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal decimal(JsonNode root, String snakeName, String camelName) {
        JsonNode node = firstExisting(root, snakeName, camelName);
        if (node == null) {
            return null;
        }
        try {
            return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static LocalDateTime dateTime(JsonNode root, String snakeName, String camelName) {
        JsonNode node = firstExisting(root, snakeName, camelName);
        if (node == null) {
            return null;
        }
        try {
            if (node.isArray() && node.size() >= 6) {
                return LocalDateTime.of(
                        node.get(0).asInt(),
                        node.get(1).asInt(),
                        node.get(2).asInt(),
                        node.get(3).asInt(),
                        node.get(4).asInt(),
                        node.get(5).asInt());
            }
            return LocalDateTime.parse(node.asText());
        } catch (Exception ignored) {
            return null;
        }
    }
}
