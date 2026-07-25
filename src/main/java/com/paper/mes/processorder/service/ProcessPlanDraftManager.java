package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanBatchItemDTO;
import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessPlanItemsBatchSaveDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessPlanDraftManager {

    private static final int STATUS_DRAFT = 0;

    private final ProcessOrderMapper orderMapper;
    private final ProcessPlanDraftStore store;
    private final ProcessPlanSaveWorkLoader workLoader;
    private final ProcessPlanDraftPreviewer previewer;
    private final BusinessLockService businessLockService;
    private final DraftOrderVersionGuard versionGuard;
    private final ProcessPlanSavePolicy savePolicy;

    public PlanPreviewVO previewProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan,
                                            Integer expectedVersion) {
        FinishConfigQuantityValidator.requireWithinLimit(plan);
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, expectedVersion);
        ProcessPlanSaveWork work = workLoader.forPreview(orderUuid, rollUuid, plan);
        ProcessPlanSaveCandidate candidate = work.candidates().getFirst();
        ProcessPlanDraftPreviewContext context = previewer.createContext(
                order, work.sourceRolls(), work.candidates());
        return previewer.preview(context, candidate.roll(), plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public PlanPreviewVO saveProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan) {
        return saveProcessPlan(orderUuid, rollUuid, plan, currentVersion(orderUuid));
    }

    @Transactional(rollbackFor = Exception.class)
    public PlanPreviewVO saveProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan,
                                         Integer expectedVersion) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, expectedVersion);
        ProcessPlanSaveWork work = workLoader.forSingleSave(orderUuid, rollUuid, plan);
        validateCandidates(work.candidates());
        ProcessPlanDraftPreviewContext context = previewer.createContext(
                order, work.sourceRolls(), work.candidates());
        PreparedProcessPlan prepared = prepare(context, work.candidates().getFirst());
        versionGuard.advance(orderUuid, expectedVersion);
        store.persist(orderUuid, prepared);
        return prepared.preview();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PlanPreviewVO> saveBatch(String orderUuid, ProcessPlanBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        savePolicy.requireDistinctTargets(dto.getOriginalUuids());
        savePolicy.requireGenericBatchAllowed(dto.getPlan());
        ProcessPlanSaveWork work = workLoader.forBatch(orderUuid, dto);
        return savePreparedBatch(order, dto.getExpectedVersion(), work);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PlanPreviewVO> saveItemsBatch(String orderUuid, ProcessPlanItemsBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        List<String> rollUuids = dto.getItems().stream()
                .map(ProcessPlanBatchItemDTO::getOriginalUuid)
                .toList();
        savePolicy.requireDistinctTargets(rollUuids);
        ProcessPlanSaveWork work = workLoader.forItems(orderUuid, dto);
        return savePreparedBatch(order, dto.getExpectedVersion(), work);
    }

    private List<PlanPreviewVO> savePreparedBatch(ProcessOrder order, Integer expectedVersion,
                                                   ProcessPlanSaveWork work) {
        validateCandidates(work.candidates());
        ProcessPlanDraftPreviewContext context = previewer.createContext(
                order, work.sourceRolls(), work.candidates());
        List<PreparedProcessPlan> prepared = work.candidates().stream()
                .map(candidate -> prepare(context, candidate))
                .toList();
        versionGuard.advance(order.getUuid(), expectedVersion);
        List<PlanPreviewVO> previews = new ArrayList<>(prepared.size());
        for (PreparedProcessPlan item : prepared) {
            store.persist(order.getUuid(), item);
            previews.add(item.preview());
        }
        return previews;
    }

    private void validateCandidates(List<ProcessPlanSaveCandidate> candidates) {
        for (ProcessPlanSaveCandidate candidate : candidates) {
            FinishConfigQuantityValidator.requireWithinLimit(candidate.plan());
            savePolicy.requireSavable(candidate);
        }
    }

    private PreparedProcessPlan prepare(ProcessPlanDraftPreviewContext context,
                                        ProcessPlanSaveCandidate candidate) {
        PlanPreviewVO preview = previewer.preview(context, candidate.roll(), candidate.plan());
        return new PreparedProcessPlan(candidate, preview);
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
}
