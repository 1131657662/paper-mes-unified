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
            item.setOriginalItems(originalItems(itemNode.get("original_items")));
            item.setProcessStepItems(processStepItems(itemNode.get("process_step_items")));
            item.setRemark(text(itemNode, "remark"));
            item.setFinishRemark(text(itemNode, "finish_remark"));
            item.setActualRemark(text(itemNode, "actual_remark"));
            items.add(item);
        }
        return items;
    }

    private static List<DeliveryDetailItemVO.OriginalSourceItem> originalItems(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<DeliveryDetailItemVO.OriginalSourceItem> items = new ArrayList<>(node.size());
        for (JsonNode itemNode : node) {
            DeliveryDetailItemVO.OriginalSourceItem item = new DeliveryDetailItemVO.OriginalSourceItem();
            item.setUuid(text(itemNode, "uuid"));
            item.setRowSort(integer(itemNode, "row_sort"));
            item.setExtraNo(text(itemNode, "extra_no"));
            item.setRollNo(text(itemNode, "roll_no"));
            item.setPaperName(text(itemNode, "paper_name"));
            item.setGramWeight(integer(itemNode, "gram_weight"));
            item.setActualGramWeight(integer(itemNode, "actual_gram_weight"));
            item.setOriginalWidth(integer(itemNode, "original_width"));
            item.setActualWidth(integer(itemNode, "actual_width"));
            item.setActualWeight(decimal(itemNode, "actual_weight"));
            item.setTotalWeight(decimal(itemNode, "total_weight"));
            item.setProcessMode(integer(itemNode, "process_mode"));
            item.setMainStepType(integer(itemNode, "main_step_type"));
            item.setMachineUuid(text(itemNode, "machine_uuid"));
            item.setOperator(text(itemNode, "operator"));
            item.setRemark(text(itemNode, "remark"));
            items.add(item);
        }
        return items;
    }

    private static List<DeliveryDetailItemVO.ProcessStepItem> processStepItems(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<DeliveryDetailItemVO.ProcessStepItem> items = new ArrayList<>(node.size());
        for (JsonNode itemNode : node) {
            DeliveryDetailItemVO.ProcessStepItem item = new DeliveryDetailItemVO.ProcessStepItem();
            item.setUuid(text(itemNode, "uuid"));
            item.setOriginalUuid(text(itemNode, "original_uuid"));
            item.setStepSort(integer(itemNode, "step_sort"));
            item.setStepType(integer(itemNode, "step_type"));
            item.setStepName(text(itemNode, "step_name"));
            item.setIsMain(integer(itemNode, "is_main"));
            item.setKnifeCount(integer(itemNode, "knife_count"));
            item.setProcessWeight(decimal(itemNode, "process_weight"));
            item.setUnitPrice(decimal(itemNode, "unit_price"));
            item.setStepAmount(decimal(itemNode, "step_amount"));
            item.setLossWeight(decimal(itemNode, "loss_weight"));
            item.setOperator(text(itemNode, "operator"));
            item.setRemark(text(itemNode, "remark"));
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
