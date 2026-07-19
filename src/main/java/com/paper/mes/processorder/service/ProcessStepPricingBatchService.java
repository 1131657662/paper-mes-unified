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
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private final ProcessOrderService processOrderService;
    private final OperationLogService operationLogService;
    private final ProcessStepPricingApprovalPolicy approvalPolicy;

    @Transactional(readOnly = true)
    public ProcessStepPricingBatchPreviewVO preview(String orderUuid, ProcessStepPricingBatchDTO dto) {
        ProcessOrder order = requireOrder(orderUuid);
        validateOrder(order, dto.getExpectedOrderVersion());
        BatchSelection selection = loadSelection(orderUuid, dto);
        return buildPreview(order, selection);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessStepPricingBatchPreviewVO apply(String orderUuid, ProcessStepPricingBatchDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
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
        Map<String, BigDecimal> prices = requestedPrices(dto);
        List<ProcessStep> steps = new ArrayList<>(processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, orderUuid)
                        .in(ProcessStep::getUuid, prices.keySet())));
        if (steps.size() != prices.size()) {
            throw new BusinessException(ErrorCode.E002, "部分工序不存在或不属于当前加工单");
        }
        Map<Integer, Set<String>> idsByType = requestedIdsByType(dto);
        for (ProcessStep step : steps) {
            if (!idsByType.getOrDefault(step.getStepType(), Set.of()).contains(step.getUuid())) {
                throw new BusinessException(ErrorCode.E001, "所选工序类型与计价分组不一致");
            }
        }
        steps.sort(java.util.Comparator.comparing(ProcessStep::getStepSort,
                java.util.Comparator.nullsLast(Integer::compareTo)).thenComparing(ProcessStep::getUuid));
        return new BatchSelection(steps, prices);
    }

    private Map<String, BigDecimal> requestedPrices(ProcessStepPricingBatchDTO dto) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        Set<Integer> types = new HashSet<>();
        for (ProcessStepPricingBatchDTO.Group group : dto.getGroups()) {
            validateGroup(group, types);
            BigDecimal price = Boolean.TRUE.equals(group.getRestoreStandard()) ? null
                    : group.getBillingUnitPrice().setScale(4, RoundingMode.HALF_UP);
            for (String uuid : group.getStepUuids()) {
                if (result.containsKey(uuid)) throw new BusinessException("工序不能重复选择");
                result.put(uuid, price);
            }
        }
        return result;
    }

    private void validateGroup(ProcessStepPricingBatchDTO.Group group, Set<Integer> types) {
        if (!types.add(group.getStepType())) throw new BusinessException("同一工序类型只能出现一次");
        if (!Boolean.TRUE.equals(group.getRestoreStandard()) && group.getBillingUnitPrice() == null) {
            throw new BusinessException("核定单价不能为空");
        }
    }

    private Map<Integer, Set<String>> requestedIdsByType(ProcessStepPricingBatchDTO dto) {
        Map<Integer, Set<String>> result = new HashMap<>();
        dto.getGroups().forEach(group -> result.put(group.getStepType(), new HashSet<>(group.getStepUuids())));
        return result;
    }

    private ProcessStepPricingBatchPreviewVO buildPreview(ProcessOrder order, BatchSelection selection) {
        List<ProcessStepPricingBatchPreviewVO.Row> rows = selection.steps().stream()
                .map(step -> ProcessStepPricingBatchCalculator.preview(step, selection.prices().get(step.getUuid())))
                .toList();
        ProcessStepPricingBatchPreviewVO vo = new ProcessStepPricingBatchPreviewVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderVersion(order.getVersion());
        vo.setStepCount(rows.size());
        vo.setRows(rows);
        vo.setStandardAmount(sum(rows, ProcessStepPricingBatchPreviewVO.Row::getStandardAmount));
        vo.setCurrentAmount(sum(rows, ProcessStepPricingBatchPreviewVO.Row::getCurrentAmount));
        vo.setFinalAmount(sum(rows, ProcessStepPricingBatchPreviewVO.Row::getFinalAmount));
        vo.setAdjustmentAmount(vo.getFinalAmount().subtract(vo.getStandardAmount()).setScale(2));
        return vo;
    }

    private BigDecimal sum(List<ProcessStepPricingBatchPreviewVO.Row> rows,
                           java.util.function.Function<ProcessStepPricingBatchPreviewVO.Row, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    private void persist(BatchSelection selection, ProcessStepPricingBatchDTO dto) {
        String operator = AuthContextHolder.currentDisplayName();
        LocalDateTime adjustedAt = LocalDateTime.now();
        String batchId = dto.getRequestId() == null || dto.getRequestId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "") : dto.getRequestId().trim();
        for (ProcessStep step : selection.steps()) {
            ConcurrencyGuard.requireRowUpdated(processStepMapper.update(null,
                    new LambdaUpdateWrapper<ProcessStep>()
                            .eq(ProcessStep::getUuid, step.getUuid())
                            .eq(ProcessStep::getVersion, step.getVersion())
                            .set(ProcessStep::getBillingUnitPrice, selection.prices().get(step.getUuid()))
                            .set(ProcessStep::getPricingAdjustmentReason, dto.getReason().trim())
                            .set(ProcessStep::getPricingAdjustedBy, operator)
                            .set(ProcessStep::getPricingAdjustedAt, adjustedAt)
                            .set(ProcessStep::getPricingAdjustmentBatchId, batchId)
                            .set(ProcessStep::getUpdateBy, operator)
                            .set(ProcessStep::getUpdateTime, adjustedAt)
                            .setSql("version = version + 1")));
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

    private void recordOperation(ProcessOrder order, ProcessStepPricingBatchDTO dto,
                                 ProcessStepPricingBatchPreviewVO preview) {
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_PRICING_ADJUST, null,
                "批量核定" + preview.getStepCount() + "道工序，调整" + preview.getAdjustmentAmount()
                        + "元：" + dto.getReason().trim());
    }

    private record BatchSelection(List<ProcessStep> steps, Map<String, BigDecimal> prices) {
    }
}
