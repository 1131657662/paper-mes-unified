package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessRouteBatchSaveDTO;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcessRouteDraftManager {

    public static final String CONFIG_TYPE_ROUTE = "routePlan";
    public static final String CONFIG_TYPE_SINGLE = "singlePlan";

    private static final int STATUS_DRAFT = 0;
    private static final int IS_REMAIN_YES = 1;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final ProcessRoutePreviewer previewer;
    private final ProcessRoutePriceResolver priceResolver;
    private final ProcessRoutePersistenceService persistenceService;
    private final ObjectMapper objectMapper;
    private final BusinessLockService businessLockService;
    private final DraftOrderVersionGuard versionGuard;
    private final ProcessRouteModePolicy routeModePolicy;

    public ProcessRoutePreviewVO preview(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        OriginalRoll roll = requireRoll(orderUuid, dto.getOriginalUuid());
        routeModePolicy.requireCompatible(roll, dto);
        return routePreview(order, roll, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessRoutePreviewVO save(String orderUuid, String rollUuid, ProcessRoutePreviewDTO dto) {
        requireSameRoll(rollUuid, dto);
        return save(orderUuid, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessRoutePreviewVO save(String orderUuid, ProcessRoutePreviewDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        versionGuard.assertLockedExpected(orderUuid, dto.getExpectedVersion());
        OriginalRoll roll = requireRoll(orderUuid, dto.getOriginalUuid());
        routeModePolicy.requireCompatible(roll, dto);
        ProcessRoutePreviewVO preview = prepare(order, roll, dto);
        versionGuard.advance(orderUuid, dto.getExpectedVersion());
        persist(order, roll, dto, preview);
        return preview;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ProcessRoutePreviewVO> saveBatch(String orderUuid, ProcessRouteBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        versionGuard.assertLockedExpected(orderUuid, dto.getExpectedVersion());
        Map<String, OriginalRoll> rolls = requireRolls(orderUuid, dto.getRoutes());
        List<PreparedRouteDraft> prepared = dto.getRoutes().stream()
                .map(route -> prepareRoute(order, rolls.get(route.getOriginalUuid()), route))
                .toList();
        versionGuard.advance(orderUuid, dto.getExpectedVersion());
        for (PreparedRouteDraft item : prepared) {
            persist(order, item.roll(), item.dto(), item.preview());
        }
        return prepared.stream().map(PreparedRouteDraft::preview).toList();
    }

    private PreparedRouteDraft prepareRoute(ProcessOrder order, OriginalRoll roll,
                                            ProcessRoutePreviewDTO dto) {
        routeModePolicy.requireCompatible(roll, dto);
        return new PreparedRouteDraft(roll, dto, prepare(order, roll, dto));
    }

    private ProcessRoutePreviewVO prepare(ProcessOrder order, OriginalRoll roll,
                                          ProcessRoutePreviewDTO dto) {
        ProcessRoutePreviewVO preview = routePreview(order, roll, dto);
        requireFinalOutputs(preview);
        return preview;
    }

    private void persist(ProcessOrder order, OriginalRoll roll, ProcessRoutePreviewDTO dto,
                         ProcessRoutePreviewVO preview) {
        updateRollRoute(roll, dto);
        upsertDraft(order.getUuid(), roll.getUuid(), dto, preview);
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
        routeModePolicy.requireCompatible(roll, dto);
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
                .isNull(OriginalRoll::getDispositionAction)
                .last("LIMIT 1"));
        if (roll == null) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        return roll;
    }

    private Map<String, OriginalRoll> requireRolls(String orderUuid, List<ProcessRoutePreviewDTO> routes) {
        List<String> ids = routes.stream().map(ProcessRoutePreviewDTO::getOriginalUuid).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(ErrorCode.E003, "链式工艺存在重复母卷");
        }
        Map<String, OriginalRoll> result = new HashMap<>();
        for (OriginalRoll roll : rollMapper.selectBatchIds(ids)) {
            if (orderUuid.equals(roll.getOrderUuid()) && roll.getDispositionAction() == null) {
                result.put(roll.getUuid(), roll);
            }
        }
        if (result.size() != ids.size()) {
            throw new BusinessException(ErrorCode.E002, "部分原纸明细不存在或不属于当前加工单");
        }
        return result;
    }

    private void requireSameRoll(String rollUuid, ProcessRoutePreviewDTO dto) {
        if (!rollUuid.equals(dto.getOriginalUuid())) {
            throw new BusinessException(ErrorCode.E003, "链式工艺来源母卷不一致");
        }
    }

    private void requireFinalOutputs(ProcessRoutePreviewVO preview) {
        boolean hasFinal = preview.getOutputs() != null
                && preview.getOutputs().stream().anyMatch(this::isDeliverableOutput);
        if (!hasFinal) {
            throw new BusinessException(ErrorCode.E003, "链式工艺至少需要一个最终成品");
        }
    }

    private boolean isDeliverableOutput(ProcessRoutePreviewVO.RouteOutputVO output) {
        return !Boolean.TRUE.equals(output.getConsumedByNextStage())
                && (output.getIsRemain() == null || output.getIsRemain() != IS_REMAIN_YES);
    }

    private void updateRollRoute(OriginalRoll roll, ProcessRoutePreviewDTO dto) {
        ProcessRoutePreviewDTO.RouteStageDTO first = dto.getStages().get(0);
        roll.setMainStepType(first.getStepType());
        roll.setMachineUuid(resolveStageMachine(first));
        roll.setUpdateBy(null);
        roll.setUpdateTime(null);
        ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
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
        draft.setProcessMode(ProcessModePolicy.STANDARD);
        draft.setMainStepType(dto.getStages().get(0).getStepType());
        draft.setConfigJson(toJson(dto));
        draft.setPreviewJson(toJson(preview));
        draft.setConfigStatus(1);
        draft.setLastError(null);
        if (draft.getUuid() == null) {
            ConcurrencyGuard.requireRowUpdated(draftMapper.insert(draft));
        } else {
            draft.setUpdateBy(null);
            draft.setUpdateTime(null);
            ConcurrencyGuard.requireRowUpdated(draftMapper.updateById(draft));
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

    private record PreparedRouteDraft(OriginalRoll roll, ProcessRoutePreviewDTO dto,
                                      ProcessRoutePreviewVO preview) {
    }
}
