package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.DraftOrderVO;
import com.paper.mes.processorder.dto.DraftSummaryVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessConfigDraftVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DraftOrderReader {

    private static final int STATUS_DRAFT = 0;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final ObjectMapper objectMapper;
    private final ProcessPlanMapper planMapper;

    public List<DraftSummaryVO> listDrafts() {
        List<ProcessOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ProcessOrder>()
                .eq(ProcessOrder::getOrderStatus, STATUS_DRAFT)
                .orderByDesc(ProcessOrder::getUpdateTime));
        if (orders.isEmpty()) {
            return List.of();
        }
        List<String> orderUuids = orders.stream().map(ProcessOrder::getUuid).toList();
        Map<String, List<OriginalRoll>> rolls = rollsByOrder(orderUuids);
        Map<String, List<ProcessConfigDraft>> configs = configsByOrder(orderUuids);
        return orders.stream().map(order -> summary(order, rolls, configs)).toList();
    }

    public DraftOrderVO getDraft(String orderUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        DraftOrderVO vo = new DraftOrderVO();
        vo.setOrder(order);
        vo.setCurrentStep(currentStep(order));
        vo.setRolls(listRolls(orderUuid));
        vo.setConfigs(listConfigVos(orderUuid));
        return vo;
    }

    private Map<String, List<OriginalRoll>> rollsByOrder(List<String> orderUuids) {
        return rollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                        .in(OriginalRoll::getOrderUuid, orderUuids))
                .stream().collect(Collectors.groupingBy(OriginalRoll::getOrderUuid, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<ProcessConfigDraft>> configsByOrder(List<String> orderUuids) {
        return draftMapper.selectList(new LambdaQueryWrapper<ProcessConfigDraft>()
                        .in(ProcessConfigDraft::getOrderUuid, orderUuids))
                .stream().collect(Collectors.groupingBy(ProcessConfigDraft::getOrderUuid, LinkedHashMap::new, Collectors.toList()));
    }

    private DraftSummaryVO summary(ProcessOrder order, Map<String, List<OriginalRoll>> rolls,
                                   Map<String, List<ProcessConfigDraft>> configs) {
        List<OriginalRoll> orderRolls = rolls.getOrDefault(order.getUuid(), List.of());
        DraftSummaryVO vo = new DraftSummaryVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerName(order.getCustomerName());
        vo.setOrderDate(order.getOrderDate());
        vo.setCurrentStep(currentStep(order));
        vo.setRollCount(orderRolls.size());
        vo.setConfiguredCount(configuredCount(configs.get(order.getUuid())));
        vo.setTotalWeight(totalWeight(orderRolls));
        return vo;
    }

    private List<OriginalRoll> listRolls(String orderUuid) {
        return rollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .orderByAsc(OriginalRoll::getRowSort));
    }

    private List<ProcessConfigDraftVO> listConfigVos(String orderUuid) {
        List<ProcessConfigDraft> drafts = draftMapper.selectList(new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid));
        List<ProcessConfigDraftVO> result = new ArrayList<>(drafts.size());
        for (ProcessConfigDraft draft : drafts) {
            result.add(configVo(draft));
        }
        return result;
    }

    private ProcessConfigDraftVO configVo(ProcessConfigDraft draft) {
        ProcessConfigDraftVO vo = new ProcessConfigDraftVO();
        vo.setOriginalUuid(draft.getOriginalUuid());
        vo.setProcessMode(draft.getProcessMode());
        vo.setMainStepType(draft.getMainStepType());
        vo.setConfigStatus(draft.getConfigStatus());
        vo.setLastError(draft.getLastError());
        if (isRouteDraft(draft)) {
            vo.setConfigType(ProcessRouteDraftManager.CONFIG_TYPE_ROUTE);
            vo.setRoute(readRoute(draft));
            vo.setRoutePreview(readRoutePreview(draft));
            return vo;
        }
        ProcessPlanDTO plan = readPlan(draft);
        vo.setConfigType(ProcessRouteDraftManager.CONFIG_TYPE_SINGLE);
        vo.setPlan(plan);
        vo.setPreview(readPreview(draft, plan));
        return vo;
    }

    private boolean isRouteDraft(ProcessConfigDraft draft) {
        try {
            return objectMapper.readTree(draft.getConfigJson()).has("stages");
        } catch (JsonProcessingException e) {
            throw new BusinessException("工艺配置草稿解析失败");
        }
    }

    private ProcessRoutePreviewDTO readRoute(ProcessConfigDraft draft) {
        try {
            return objectMapper.readValue(draft.getConfigJson(), ProcessRoutePreviewDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("链式工艺草稿解析失败");
        }
    }

    private ProcessRoutePreviewVO readRoutePreview(ProcessConfigDraft draft) {
        if (draft.getPreviewJson() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(draft.getPreviewJson(), ProcessRoutePreviewVO.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private ProcessPlanDTO readPlan(ProcessConfigDraft draft) {
        try {
            JsonNode node = objectMapper.readTree(draft.getConfigJson());
            if (node.has("rewindSegments")) {
                FinishConfigSaveDTO legacy = objectMapper.treeToValue(node, FinishConfigSaveDTO.class);
                return planMapper.fromSaveDto(legacy);
            }
            return objectMapper.treeToValue(node, ProcessPlanDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("工艺配置草稿解析失败");
        }
    }

    private PlanPreviewVO readPreview(ProcessConfigDraft draft, ProcessPlanDTO plan) {
        if (draft.getPreviewJson() == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(draft.getPreviewJson());
            if (node.has("ready")) {
                return objectMapper.treeToValue(node, PlanPreviewVO.class);
            }
            FinishPreviewVO legacy = objectMapper.treeToValue(node, FinishPreviewVO.class);
            return planMapper.toPlanPreview(plan, draft.getOriginalUuid(), legacy);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private int configuredCount(List<ProcessConfigDraft> configs) {
        if (configs == null) {
            return 0;
        }
        return (int) configs.stream().filter(item -> item.getConfigStatus() != null && item.getConfigStatus() == 1).count();
    }

    private BigDecimal totalWeight(List<OriginalRoll> rolls) {
        return rolls.stream()
                .map(OriginalRoll::getTotalWeight)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer currentStep(ProcessOrder order) {
        return order.getExtNum1() == null ? 0 : order.getExtNum1().intValue();
    }
}
