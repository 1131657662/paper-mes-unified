package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** 旧快照弱类型字段读取。 */
final class LegacySnapshotValues {

    private LegacySnapshotValues() {
    }

    static String text(JsonNode node, String key, String fallback) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    static Integer integer(JsonNode node, Integer fallback) {
        if (node == null || node.isNull()) return fallback;
        return node.asInt();
    }

    static BigDecimal decimal(JsonNode node, BigDecimal fallback) {
        return node == null || node.isNull() ? fallback : node.decimalValue();
    }

    static LocalDateTime dateTime(JsonNode node, LocalDateTime fallback) {
        if (node == null || node.isNull()) return fallback;
        try {
            return LocalDateTime.parse(node.asText());
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }
}
