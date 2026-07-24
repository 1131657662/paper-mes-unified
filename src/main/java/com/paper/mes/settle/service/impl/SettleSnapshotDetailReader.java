package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.settle.entity.SettleDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads frozen settle detail snapshots from current camelCase fields and legacy snake_case fields.
 */
@Slf4j
final class SettleSnapshotDetailReader {

    private SettleSnapshotDetailReader() {
    }

    static List<SettleDetail> read(String snapBill, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(snapBill)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(snapBill);
            for (String name : List.of("detail_items", "detailItems", "details")) {
                List<SettleDetail> items = readNode(root.get(name), objectMapper);
                if (hasUsableSnapshotItems(items)) {
                    return items;
                }
            }
            throw corruptedSnapshot();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("结算历史明细快照解析失败：{}", ex.getMessage());
            throw corruptedSnapshot();
        }
    }

    private static BusinessException corruptedSnapshot() {
        return new BusinessException(ErrorCode.E008,
                "结算单历史明细快照损坏，已禁止使用当前结算数据替代，请联系管理员处理");
    }

    private static List<SettleDetail> readNode(JsonNode node, ObjectMapper objectMapper) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<SettleDetail> items = readCamelCaseItems(node, objectMapper);
        if (hasUsableSnapshotItems(items)) {
            return items;
        }
        return readSnakeCaseItems(node);
    }

    private static List<SettleDetail> readCamelCaseItems(JsonNode node, ObjectMapper objectMapper) {
        try {
            return objectMapper.convertValue(node, new TypeReference<List<SettleDetail>>() {});
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<SettleDetail> readSnakeCaseItems(JsonNode node) {
        List<SettleDetail> items = new ArrayList<>(node.size());
        for (JsonNode itemNode : node) {
            SettleDetail item = new SettleDetail();
            item.setUuid(text(itemNode, "uuid"));
            item.setSettleUuid(text(itemNode, "settle_uuid"));
            item.setOrderUuid(text(itemNode, "order_uuid"));
            item.setOrderNo(text(itemNode, "order_no"));
            item.setSawAmount(decimal(itemNode, "saw_amount"));
            item.setRewindAmount(decimal(itemNode, "rewind_amount"));
            item.setServiceAmount(decimal(itemNode, "service_amount"));
            item.setStandardProcessAmount(decimal(itemNode, "standard_process_amount"));
            item.setPricingAdjustmentAmount(decimal(itemNode, "pricing_adjustment_amount"));
            item.setPricingAdjustmentReason(text(itemNode, "pricing_adjustment_reason"));
            item.setExtraAmount(decimal(itemNode, "extra_amount"));
            item.setOrderAmount(decimal(itemNode, "order_amount"));
            item.setRemark(text(itemNode, "remark"));
            items.add(item);
        }
        return items;
    }

    private static boolean hasUsableSnapshotItems(List<SettleDetail> items) {
        return items != null && !items.isEmpty()
                && items.stream().anyMatch(item -> StringUtils.hasText(item.getOrderUuid())
                || StringUtils.hasText(item.getOrderNo()));
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
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
