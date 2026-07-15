package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;

import java.util.List;
import java.util.Map;

/** 完整打印详情在快照中的编解码。 */
final class ProcessOrderSnapshotDetailCodec {

    private ProcessOrderSnapshotDetailCodec() {
    }

    static void append(Map<String, Object> snapshot, ProcessOrderDetailVO detail,
                       ObjectMapper objectMapper) {
        JsonNode node = objectMapper.valueToTree(detail);
        JsonNode order = node.get("order");
        if (order instanceof ObjectNode objectNode) {
            objectNode.remove(List.of("snapPrint", "snapFinish"));
        }
        snapshot.put("detail", node);
    }

    static ProcessOrderDetailVO read(JsonNode root, ObjectMapper objectMapper) {
        JsonNode detail = root.get("detail");
        if (detail == null || !detail.isObject()) {
            return null;
        }
        try {
            ProcessOrderDetailVO result = objectMapper.convertValue(detail, ProcessOrderDetailVO.class);
            if (result.getOrder() == null || result.getOrder().getUuid() == null) {
                throw corruptedSnapshot();
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw corruptedSnapshot();
        }
    }

    static ProcessOrderDetailVO copy(ProcessOrderDetailVO detail, ObjectMapper objectMapper) {
        return objectMapper.convertValue(objectMapper.valueToTree(detail), ProcessOrderDetailVO.class);
    }

    private static BusinessException corruptedSnapshot() {
        return new BusinessException(ErrorCode.E008, "加工单打印快照损坏，请联系管理员处理");
    }
}
