package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
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
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcessRouteDraftManager {

    public static final String CONFIG_TYPE_ROUTE = "routePlan";
    public static final String CONFIG_TYPE_SINGLE = "singlePlan";

    private static final int STATUS_DRAFT = 0;
    private static final int PROCESS_MODE_STANDARD = 1;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final ProcessRoutePreviewer previewer;
    private final ProcessRoutePriceResolver priceResolver;
    private final ProcessRoutePersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    public ProcessRoutePreviewVO preview(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessOrder order = requireDraft(orderUuid);
        OriginalRoll roll = requireRoll(orderUuid, dto.getOriginalUuid());
        return routePreview(order, roll, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessRoutePreviewVO save(String orderUuid, String rollUuid, ProcessRoutePreviewDTO dto) {
        requireSameRoll(rollUuid, dto);
        return save(orderUuid, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessRoutePreviewVO save(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessOrder order = requireDraft(orderUuid);
        OriginalRoll roll = requireRoll(orderUuid, dto.getOriginalUuid());
        ProcessRoutePreviewVO preview = routePreview(order, roll, dto);
        requireFinalOutputs(preview);
        updateRollRoute(roll, dto);
        upsertDraft(orderUuid, roll.getUuid(), dto, preview);
        return preview;
    }

    public boolean isRouteDraft(ProcessConfigDraft draft) {
        try {
            return objectMapper.readTree(draft.getConfigJson()).has("stages");
        } catch (JsonProcessingException e) {
            throw new BusinessException("链式工艺草稿解析失败");
        }
    }

    public ProcessRoutePreviewDTO readRouteDraft(ProcessConfigDraft draft) {
        try {
            return objectMapper.readValue(draft.getConfigJson(), ProcessRoutePreviewDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("链式工艺草稿解析失败");
        }
    }

    public ProcessRoutePreviewVO submit(ProcessOrder order, OriginalRoll roll, ProcessConfigDraft draft) {
        ProcessRoutePreviewDTO dto = readRouteDraft(draft);
        ProcessRoutePreviewVO preview = routePreview(order, roll, dto);
        requireFinalOutputs(preview);
        persistenceService.replaceRoute(new ProcessRouteContext(order, roll), dto, preview);
        return preview;
    }

    private ProcessRoutePreviewVO routePreview(ProcessOrder order, OriginalRoll roll, ProcessRoutePreviewDTO dto) {
        priceResolver.applyDefaultPrices(order, dto);
        return previewer.preview(roll, dto);
    }

    private ProcessOrder requireDraft(String orderUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可配置链式工艺");
        }
        return order;
    }

    private OriginalRoll requireRoll(String orderUuid, String rollUuid) {
        OriginalRoll roll = rollMapper.selectOne(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .eq(OriginalRoll::getUuid, rollUuid)
                .last("LIMIT 1"));
        if (roll == null) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        return roll;
    }

    private void requireSameRoll(String rollUuid, ProcessRoutePreviewDTO dto) {
        if (!rollUuid.equals(dto.getOriginalUuid())) {
            throw new BusinessException(ErrorCode.E003, "链式工艺来源母卷不一致");
        }
    }

    private void requireFinalOutputs(ProcessRoutePreviewVO preview) {
        boolean hasFinal = preview.getOutputs() != null
                && preview.getOutputs().stream().anyMatch(item -> !Boolean.TRUE.equals(item.getConsumedByNextStage()));
        if (!hasFinal) {
            throw new BusinessException(ErrorCode.E003, "链式工艺至少需要一个最终成品");
        }
    }

    private void updateRollRoute(OriginalRoll roll, ProcessRoutePreviewDTO dto) {
        ProcessRoutePreviewDTO.RouteStageDTO first = dto.getStages().get(0);
        roll.setProcessMode(PROCESS_MODE_STANDARD);
        roll.setMainStepType(first.getStepType());
        roll.setMachineUuid(resolveStageMachine(first));
        rollMapper.updateById(roll);
    }

    private String resolveStageMachine(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getMachineUuid() != null && !stage.getMachineUuid().isBlank()) {
            return stage.getMachineUuid();
        }
        return stage.getPlan() == null ? null : stage.getPlan().getMachineUuid();
    }

    private void upsertDraft(String orderUuid, String rollUuid, ProcessRoutePreviewDTO dto, ProcessRoutePreviewVO preview) {
        ProcessConfigDraft draft = selectDraft(orderUuid, rollUuid);
        if (draft == null) {
            draft = new ProcessConfigDraft();
            draft.setOrderUuid(orderUuid);
            draft.setOriginalUuid(rollUuid);
        }
        draft.setProcessMode(PROCESS_MODE_STANDARD);
        draft.setMainStepType(dto.getStages().get(0).getStepType());
        draft.setConfigJson(toJson(dto));
        draft.setPreviewJson(toJson(preview));
        draft.setConfigStatus(1);
        draft.setLastError(null);
        if (draft.getUuid() == null) {
            draftMapper.insert(draft);
        } else {
            draftMapper.updateById(draft);
        }
    }

    private ProcessConfigDraft selectDraft(String orderUuid, String rollUuid) {
        return draftMapper.selectOne(new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid)
                .eq(ProcessConfigDraft::getOriginalUuid, rollUuid)
                .last("LIMIT 1"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("链式工艺草稿序列化失败");
        }
    }
}
