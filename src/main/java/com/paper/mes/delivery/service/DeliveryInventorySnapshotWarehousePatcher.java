package com.paper.mes.delivery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.warehouse.entity.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 将人工确认的仓库补入完成快照，避免实体库存与历史快照分叉。 */
@Component
@RequiredArgsConstructor
public class DeliveryInventorySnapshotWarehousePatcher {

    private final ObjectMapper objectMapper;

    public String patch(String snapshot, Warehouse warehouse, Set<String> finishUuids) {
        if (snapshot == null || snapshot.isBlank()) {
            return snapshot;
        }
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            if (!(root instanceof ObjectNode objectRoot)) {
                throw corruptedSnapshot();
            }
            patchWarehouse(objectRoot, warehouse);
            patchFinishRolls(objectRoot.path("finish_rolls"), warehouse.getUuid(), finishUuids);
            patchDetail(objectRoot.path("detail"), warehouse.getUuid(), finishUuids);
            return objectMapper.writeValueAsString(objectRoot);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw corruptedSnapshot();
        }
    }

    private void patchWarehouse(ObjectNode root, Warehouse warehouse) {
        JsonNode existing = root.get("warehouse");
        ObjectNode warehouseNode;
        if (existing instanceof ObjectNode objectNode) {
            warehouseNode = objectNode;
        } else {
            warehouseNode = objectMapper.createObjectNode();
            root.set("warehouse", warehouseNode);
        }
        ensureMatch(warehouseNode.get("uuid"), warehouse.getUuid(), "完成快照已有其他仓库归属");
        warehouseNode.put("uuid", warehouse.getUuid());
        warehouseNode.put("name", warehouse.getWarehouseName());
        if (warehouse.getLocation() == null) warehouseNode.putNull("location");
        else warehouseNode.put("location", warehouse.getLocation());
    }

    private void patchFinishRolls(JsonNode finishes, String warehouseUuid, Set<String> finishUuids) {
        if (!finishes.isArray()) return;
        for (JsonNode finish : finishes) {
            if (finish instanceof ObjectNode node) {
                if (finishUuids.contains(text(node, "uuid"))) {
                    ensureMatch(node.get("warehouse_uuid"), warehouseUuid, "完成快照成品已有其他仓库归属");
                    node.put("warehouse_uuid", warehouseUuid);
                }
            }
        }
    }

    private void patchDetail(JsonNode detail, String warehouseUuid, Set<String> finishUuids) {
        if (!(detail instanceof ObjectNode detailNode)) return;
        JsonNode order = detailNode.path("order");
        if (order instanceof ObjectNode orderNode) {
            ensureMatch(orderNode.get("warehouseUuid"), warehouseUuid, "完成快照加工单已有其他仓库归属");
            orderNode.put("warehouseUuid", warehouseUuid);
        }
        JsonNode finishes = detailNode.path("finishRolls");
        if (!finishes.isArray()) return;
        for (JsonNode finish : finishes) {
            if (finish instanceof ObjectNode node) {
                if (finishUuids.contains(text(node, "uuid"))) {
                    ensureMatch(node.get("warehouseUuid"), warehouseUuid, "完成快照成品已有其他仓库归属");
                    node.put("warehouseUuid", warehouseUuid);
                }
            }
        }
    }

    private void ensureMatch(JsonNode existing, String expected, String message) {
        if (existing != null && !existing.isNull() && !existing.asText().isBlank()
                && !expected.equals(existing.asText())) {
            throw new BusinessException(ErrorCode.E003, message);
        }
    }

    private String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BusinessException corruptedSnapshot() {
        return new BusinessException(ErrorCode.E008, "完成快照损坏，无法自动补录仓库");
    }
}
