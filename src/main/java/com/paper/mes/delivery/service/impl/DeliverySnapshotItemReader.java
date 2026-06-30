package com.paper.mes.delivery.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 出库快照明细读取：优先读取当前 camelCase 快照，兼容旧版 snake_case details。
 */
final class DeliverySnapshotItemReader {

    private DeliverySnapshotItemReader() {
    }

    static List<DeliveryDetailItemVO> read(String snapDelivery, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(snapDelivery)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(snapDelivery);
            JsonNode node = firstExisting(root, "detail_items", "detailItems", "details");
            if (node == null || !node.isArray()) {
                return null;
            }
            List<DeliveryDetailItemVO> items = readCamelCaseItems(node, objectMapper);
            if (hasUsableSnapshotItems(items)) {
                return items;
            }
            items = readSnakeCaseItems(node);
            return hasUsableSnapshotItems(items) ? items : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<DeliveryDetailItemVO> readCamelCaseItems(JsonNode node, ObjectMapper objectMapper) {
        try {
            return objectMapper.convertValue(node, new TypeReference<List<DeliveryDetailItemVO>>() {});
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<DeliveryDetailItemVO> readSnakeCaseItems(JsonNode node) {
        List<DeliveryDetailItemVO> items = new ArrayList<>(node.size());
        for (JsonNode itemNode : node) {
            DeliveryDetailItemVO item = new DeliveryDetailItemVO();
            item.setUuid(text(itemNode, "uuid"));
            item.setDeliveryUuid(text(itemNode, "delivery_uuid"));
            item.setFinishUuid(text(itemNode, "finish_uuid"));
            item.setOrderUuid(text(itemNode, "order_uuid"));
            item.setOrderNo(text(itemNode, "order_no"));
            item.setFinishRollNo(text(itemNode, "finish_roll_no"));
            item.setPaperName(text(itemNode, "paper_name"));
            item.setGramWeight(integer(itemNode, "gram_weight"));
            item.setFinishWidth(integer(itemNode, "finish_width"));
            item.setFinishDiameter(integer(itemNode, "finish_diameter"));
            item.setFinishCoreDiameter(integer(itemNode, "finish_core_diameter"));
            item.setActualWeight(decimal(itemNode, "actual_weight"));
            item.setOutWeight(decimal(itemNode, "out_weight"));
            item.setSourceType(integer(itemNode, "source_type"));
            item.setFinishStatus(integer(itemNode, "finish_status"));
            item.setOriginalRollNos(text(itemNode, "original_roll_nos"));
            item.setOriginalSummary(text(itemNode, "original_summary"));
            item.setProcessModeText(text(itemNode, "process_mode_text"));
            item.setProcessSummary(text(itemNode, "process_summary"));
            item.setRemark(text(itemNode, "remark"));
            item.setFinishRemark(text(itemNode, "finish_remark"));
            item.setActualRemark(text(itemNode, "actual_remark"));
            items.add(item);
        }
        return items;
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

    private static boolean hasUsableSnapshotItems(List<DeliveryDetailItemVO> items) {
        return items != null && !items.isEmpty()
                && items.stream().anyMatch(item -> StringUtils.hasText(item.getFinishUuid())
                || StringUtils.hasText(item.getFinishRollNo()));
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber() ? value.asInt() : Integer.valueOf(value.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
