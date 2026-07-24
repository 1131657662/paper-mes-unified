package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanBatchItemDTO;
import com.paper.mes.processorder.dto.ProcessPlanItemsBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessPlanDraftManager {

    private static final int STATUS_DRAFT = 0;
    private static final int REWIND_MODE_MULTI_SOURCE = 5;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final ProcessOrderService orderService;
    private final ProcessPlanMapper planMapper;
    private final SawPlanPreviewer sawPlanPreviewer;
    private final OnSitePlanPreviewer onSitePlanPreviewer;
    private final ObjectMapper objectMapper;
    private final BusinessLockService businessLockService;
    private final ServiceOnlyProcessPolicy serviceOnlyProcessPolicy;
    private final DraftOrderVersionGuard versionGuard;

    public PlanPreviewVO previewProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan,
                                            Integer expectedVersion) {
        FinishConfigQuantityValidator.requireWithinLimit(plan);
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, expectedVersion);
        OriginalRoll roll = requireRoll(orderUuid, rollUuid);
        return previewOnly(orderUuid, roll, plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public PlanPreviewVO saveProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan) {
        return saveProcessPlan(orderUuid, rollUuid, plan, currentVersion(orderUuid));
    }

    @Transactional(rollbackFor = Exception.class)
    public PlanPreviewVO saveProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan,
                                         Integer expectedVersion) {
        FinishConfigQuantityValidator.requireWithinLimit(plan);
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, expectedVersion);
        versionGuard.advance(orderUuid, expectedVersion);
        OriginalRoll roll = requireRoll(orderUuid, rollUuid);
        PlanPreviewVO preview = saveOne(orderUuid, roll, plan);
        return preview;
    }

    private PlanPreviewVO saveOne(String orderUuid, OriginalRoll roll, ProcessPlanDTO plan) {
        updateRollProcess(roll, plan);
        PlanPreviewVO preview = previewOnly(orderUuid, roll, plan);
        upsertDraft(orderUuid, roll.getUuid(), plan, preview);
        return preview;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PlanPreviewVO> saveBatch(String orderUuid, ProcessPlanBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        versionGuard.advance(orderUuid, dto.getExpectedVersion());
        List<PlanPreviewVO> previews = new ArrayList<>(dto.getOriginalUuids().size());
        for (String rollUuid : dto.getOriginalUuids()) {
            OriginalRoll roll = requireRoll(orderUuid, rollUuid);
            ProcessPlanDTO plan = planForTargetRoll(dto.getPlan(), rollUuid);
            FinishConfigQuantityValidator.requireWithinLimit(plan);
            previews.add(saveOne(orderUuid, roll, plan));
        }
        return previews;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PlanPreviewVO> saveItemsBatch(String orderUuid, ProcessPlanItemsBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        ensureDistinctItems(dto.getItems());
        versionGuard.advance(orderUuid, dto.getExpectedVersion());
        List<PlanPreviewVO> previews = new ArrayList<>(dto.getItems().size());
        for (ProcessPlanBatchItemDTO item : dto.getItems()) {
            FinishConfigQuantityValidator.requireWithinLimit(item.getPlan());
            OriginalRoll roll = requireRoll(orderUuid, item.getOriginalUuid());
            previews.add(saveOne(orderUuid, roll, item.getPlan()));
        }
        return previews;
    }

    private void ensureDistinctItems(List<ProcessPlanBatchItemDTO> items) {
        long distinctCount = items.stream().map(ProcessPlanBatchItemDTO::getOriginalUuid).distinct().count();
        if (distinctCount != items.size()) {
            throw new BusinessException("批量加工方案存在重复母卷");
        }
    }

    private ProcessPlanDTO planForTargetRoll(ProcessPlanDTO plan, String rollUuid) {
        ProcessPlanDTO copy = objectMapper.convertValue(plan, ProcessPlanDTO.class);
        if (copy.getRewindMode() != null && copy.getRewindMode() == REWIND_MODE_MULTI_SOURCE) {
            return copy;
        }
        rebaseFinishSources(copy.getFinishSpecs(), rollUuid);
        rebaseSegmentSources(copy.getSegments(), rollUuid);
        return copy;
    }

    private void rebaseFinishSources(List<FinishConfigSpecDTO> specs, String rollUuid) {
        if (specs == null) {
            return;
        }
        for (FinishConfigSpecDTO spec : specs) {
            FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
            source.setOriginalUuid(rollUuid);
            source.setShareRatio(BigDecimal.valueOf(100));
            source.setConsumeRatio(BigDecimal.valueOf(100));
            spec.setSources(List.of(source));
        }
    }

    private void rebaseSegmentSources(List<RewindSegmentPlanDTO> segments, String rollUuid) {
        if (segments == null) {
            return;
        }
        for (RewindSegmentPlanDTO segment : segments) {
            RewindSourcePlanDTO source = new RewindSourcePlanDTO();
            source.setOriginalUuid(rollUuid);
            source.setSourceSort(1);
            source.setShareRatio(BigDecimal.valueOf(100));
            source.setConsumeRatio(BigDecimal.valueOf(100));
            segment.setSources(List.of(source));
        }
    }

    private PlanPreviewVO previewOnly(String orderUuid, OriginalRoll roll, ProcessPlanDTO plan) {
        ProcessModePolicy.requireValid(plan.getProcessMode(), plan.getMainStepType());
        if (ProcessModePolicy.isDirectShip(plan.getProcessMode())) {
            return planMapper.directPreview(plan, roll.getUuid());
        }
        if (ProcessModePolicy.isServiceOnly(plan.getProcessMode())) {
            int finishCount = roll.getPieceNum() == null ? 1 : roll.getPieceNum();
            return planMapper.serviceOnlyPreview(plan, roll.getUuid(), finishCount,
                    serviceOnlyProcessPolicy.hasConfiguredStep(roll.getUuid()));
        }
        if (plan.getProcessMode() != null && plan.getProcessMode() == ProcessModePolicy.ON_SITE) {
            return onSitePlanPreviewer.preview(plan, roll.getUuid());
        }
        if (plan.getMainStepType() != null && plan.getMainStepType() == FeeCalculator.STEP_TYPE_SAW) {
            return sawPlanPreviewer.preview(plan, roll);
        }
        try {
            FinishPreviewVO preview = orderService.previewRewindPlan(orderUuid, roll.getUuid(), planMapper.toPreviewDto(plan));
            return planMapper.toPlanPreview(plan, roll.getUuid(), preview);
        } catch (BusinessException e) {
            return errorPreview(plan, roll.getUuid(), e.getMessage());
        }
    }

    private PlanPreviewVO errorPreview(ProcessPlanDTO plan, String rollUuid, String message) {
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setOriginalUuid(rollUuid);
        preview.setProcessMode(plan.getProcessMode());
        preview.setMainStepType(plan.getMainStepType());
        preview.setRewindMode(plan.getRewindMode());
        preview.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        preview.setReady(false);
        preview.getErrors().add(message);
        preview.setSummary("方案存在错误，请按提示修正");
        return preview;
    }

    private void upsertDraft(String orderUuid, String rollUuid, ProcessPlanDTO plan, PlanPreviewVO preview) {
        ProcessConfigDraft draft = selectDraft(orderUuid, rollUuid);
        if (draft == null) {
            draft = new ProcessConfigDraft();
            draft.setOrderUuid(orderUuid);
            draft.setOriginalUuid(rollUuid);
        }
        draft.setProcessMode(plan.getProcessMode());
        draft.setMainStepType(plan.getMainStepType());
        draft.setConfigJson(toJson(plan));
        draft.setPreviewJson(toJson(preview));
        draft.setConfigStatus(preview.isReady() ? 1 : 0);
        draft.setLastError(preview.isReady() ? null : String.join("；", preview.getErrors()));
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

    private void updateRollProcess(OriginalRoll roll, ProcessPlanDTO plan) {
        roll.setProcessMode(plan.getProcessMode());
        roll.setMainStepType(plan.getMainStepType());
        roll.setMachineUuid(plan.getMachineUuid());
        rollMapper.updateById(roll);
    }

    private ProcessOrder requireDraft(String orderUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可编辑");
        }
        return order;
    }

    private Integer currentVersion(String orderUuid) {
        return requireDraft(orderUuid).getVersion();
    }

    private OriginalRoll requireRoll(String orderUuid, String rollUuid) {
        OriginalRoll roll = rollMapper.selectById(rollUuid);
        if (roll == null || !orderUuid.equals(roll.getOrderUuid())) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        return roll;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("工艺配置草稿序列化失败");
        }
    }
}
