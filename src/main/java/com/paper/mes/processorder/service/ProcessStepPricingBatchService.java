package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchPreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessStepPricingBatchService {

    private static final Set<Integer> ADJUSTABLE_STATUSES = Set.of(3, 4);
    private final BusinessLockService businessLockService;
    private final ProcessStepMapper processStepMapper;
    private final OriginalRollMapper originalRollMapper;
    private final SettleDetailMapper settleDetailMapper;
    private final ProcessOrderService processOrderService;
    private final OperationLogService operationLogService;
    private final ProcessStepPricingApprovalPolicy approvalPolicy;

    @Transactional(readOnly = true)
    public ProcessStepPricingBatchPreviewVO preview(String orderUuid, ProcessStepPricingBatchDTO dto) {
        ProcessOrder order = requireOrder(orderUuid);
        ensureOrderNotSettled(orderUuid);
        validateOrder(order, dto.getExpectedOrderVersion());
        BatchSelection selection = loadSelection(orderUuid, dto);
        return buildPreview(order, selection);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessStepPricingBatchPreviewVO apply(String orderUuid, ProcessStepPricingBatchDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
        ensureOrderNotSettled(orderUuid);
        validateOrder(order, dto.getExpectedOrderVersion());
        BatchSelection selection = loadSelection(orderUuid, dto);
        ProcessStepPricingBatchPreviewVO preview = buildPreview(order, selection);
        persist(selection, dto);
        FeeResultVO result = processOrderService.calcFee(orderUuid);
        approvalPolicy.requireForOrder(result);
        recordOperation(order, dto, preview);
        ProcessOrder updated = requireOrder(orderUuid);
        preview.setOrderVersion(updated.getVersion());
        return preview;
    }

    private BatchSelection loadSelection(String orderUuid, ProcessStepPricingBatchDTO dto) {
        Map<String, ProcessStepPricingBatchRequestResolver.Change> changes =
                ProcessStepPricingBatchRequestResolver.resolve(dto);
        List<ProcessStep> steps = new ArrayList<>(processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, orderUuid)
                        .in(ProcessStep::getUuid, changes.keySet())));
        if (steps.size() != changes.size()) {
            throw new BusinessException(ErrorCode.E002, "部分工序不存在或不属于当前加工单");
        }
        for (ProcessStep step : steps) {
            ProcessStepPricingBatchRequestResolver.Change change = changes.get(step.getUuid());
            if (change == null || !java.util.Objects.equals(change.stepType(), step.getStepType())) {
                throw new BusinessException(ErrorCode.E001, "所选工序类型与计价分组不一致");
            }
        }
        steps.sort(java.util.Comparator.comparing(ProcessStep::getStepSort,
                java.util.Comparator.nullsLast(Integer::compareTo)).thenComparing(ProcessStep::getUuid));
        return new BatchSelection(steps, changes, loadOriginalRolls(steps));
    }

    private ProcessStepPricingBatchPreviewVO buildPreview(ProcessOrder order, BatchSelection selection) {
        return ProcessStepPricingBatchPreviewFactory.build(
                order, selection.steps(), selection.changes(), selection.rolls());
    }

    private Map<String, OriginalRoll> loadOriginalRolls(List<ProcessStep> steps) {
        List<String> ids = steps.stream()
                .filter(step -> step.getStepType() != null && step.getStepType() >= 3)
                .map(ProcessStep::getOriginalUuid).filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        Map<String, OriginalRoll> result = new LinkedHashMap<>();
        originalRollMapper.selectBatchIds(ids).forEach(roll -> result.put(roll.getUuid(), roll));
        return result;
    }

    private void persist(BatchSelection selection, ProcessStepPricingBatchDTO dto) {
        String operator = AuthContextHolder.currentDisplayName();
        LocalDateTime adjustedAt = LocalDateTime.now();
        String batchId = dto.getRequestId() == null || dto.getRequestId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "") : dto.getRequestId().trim();
        for (ProcessStep step : selection.steps()) {
            ProcessStepPricingBatchRequestResolver.Change change = selection.changes().get(step.getUuid());
            LambdaUpdateWrapper<ProcessStep> update = new LambdaUpdateWrapper<ProcessStep>()
                    .eq(ProcessStep::getUuid, step.getUuid())
                    .eq(ProcessStep::getVersion, step.getVersion());
            applyChange(update, change);
            ConcurrencyGuard.requireRowUpdated(processStepMapper.update(null,
                    update
                            .set(ProcessStep::getPricingAdjustmentReason, dto.getReason().trim())
                            .set(ProcessStep::getPricingAdjustedBy, operator)
                            .set(ProcessStep::getPricingAdjustedAt, adjustedAt)
                            .set(ProcessStep::getPricingAdjustmentBatchId, batchId)
                            .set(ProcessStep::getUpdateBy, operator)
                            .set(ProcessStep::getUpdateTime, adjustedAt)
                            .setSql("version = version + 1")));
        }
    }

    private void applyChange(LambdaUpdateWrapper<ProcessStep> update,
                             ProcessStepPricingBatchRequestResolver.Change change) {
        update.set(ProcessStep::getBillingUnitPrice, change.billingUnitPrice());
        if (change.billingMode() == null) return;
        update.set(ProcessStep::getBillingMode, change.billingMode())
                .set(ProcessStep::getBillingQuantity, null)
                .set(ProcessStep::getBillingAmount, change.billingAmount());
        if (change.billingMode() == ProcessStepPricingPolicy.STANDARD) {
            update.set(ProcessStep::getBillingBasis, change.billingBasis());
        }
    }

    private ProcessOrder requireOrder(String orderUuid) {
        ProcessOrder order = processOrderService.getById(orderUuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "加工单不存在");
        return order;
    }

    private void validateOrder(ProcessOrder order, Integer expectedVersion) {
        if (!ADJUSTABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E001, "仅待回录或已完成加工单可调整计价");
        }
        if (!java.util.Objects.equals(order.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.E006, "加工单已被他人修改，请刷新后重试");
        }
    }

    private void ensureOrderNotSettled(String orderUuid) {
        Long count = settleDetailMapper.selectCount(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getOrderUuid, orderUuid));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.E001, "加工单已生成结算单，请先作废结算单再调整服务费");
        }
    }

    private void recordOperation(ProcessOrder order, ProcessStepPricingBatchDTO dto,
                                 ProcessStepPricingBatchPreviewVO preview) {
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_PRICING_ADJUST, null,
                "批量核定" + preview.getStepCount() + "道工序，调整" + preview.getAdjustmentAmount()
                        + "元：" + dto.getReason().trim());
    }

    private record BatchSelection(
            List<ProcessStep> steps,
            Map<String, ProcessStepPricingBatchRequestResolver.Change> changes,
            Map<String, OriginalRoll> rolls) {
    }
}
