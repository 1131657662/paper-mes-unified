package com.paper.mes.processorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProcessOrderAppendJson {

    private final ObjectMapper objectMapper;

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加工艺配置序列化失败");
        }
    }

    public FinishConfigSaveDTO readPlan(String json) {
        try {
            return objectMapper.readValue(json, FinishConfigSaveDTO.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加工艺配置无法解析");
        }
    }

    public ProcessRoutePreviewDTO readRoute(String json) {
        try {
            return objectMapper.readValue(json, ProcessRoutePreviewDTO.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加链式工艺无法解析");
        }
    }

    public PlanPreviewVO readPreview(String json) {
        try {
            return objectMapper.readValue(json, PlanPreviewVO.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加工艺预览无法解析");
        }
    }

    public List<ProcessStepDTO> readServiceSteps(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ProcessStepDTO.class));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加附加工艺无法解析");
        }
    }

    public String replaceOriginalUuids(String json, Map<String, String> replacements) {
        try {
            JsonNode root = objectMapper.readTree(json);
            replaceNode(root, replacements);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加工艺配置无法解析");
        }
    }

    public boolean referencesAnyOriginalUuid(String json, Set<String> originalUuids) {
        if (json == null || json.isBlank() || originalUuids.isEmpty()) return false;
        try {
            return referencesAny(objectMapper.readTree(json), originalUuids);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("追加工艺配置无法解析");
        }
    }

    private void replaceNode(JsonNode node, Map<String, String> replacements) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            JsonNode originalUuid = object.get("originalUuid");
            if (originalUuid != null && originalUuid.isTextual()) {
                String replacement = replacements.get(originalUuid.asText());
                if (replacement != null) object.put("originalUuid", replacement);
            }
            object.elements().forEachRemaining(child -> replaceNode(child, replacements));
            return;
        }
        if (node.isArray()) node.elements().forEachRemaining(child -> replaceNode(child, replacements));
    }

    private boolean referencesAny(JsonNode node, Set<String> originalUuids) {
        if (node == null) return false;
        if (node.isObject()) {
            JsonNode originalUuid = node.get("originalUuid");
            if (originalUuid != null && originalUuid.isTextual()
                    && originalUuids.contains(originalUuid.asText())) return true;
        }
        for (JsonNode child : node) {
            if (referencesAny(child, originalUuids)) return true;
        }
        return false;
    }
}
