package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.remain.dto.RemainAdjustmentNextSettleDTO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainAdjustmentLine;
import com.paper.mes.remain.entity.RemainApplication;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.mapper.RemainAdjustmentLineMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.mapper.RemainApplicationMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainAdjustmentNextSettlementService {

    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainAdjustmentLineMapper adjustmentLineMapper;
    private final RemainApplicationMapper applicationMapper;
    private final RemainRegistrationMapper registrationMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final BusinessLockService businessLockService;
    private final RemainLockService remainLockService;
    private final RemainApplicationTargetValidator targetValidator;
    private final RemainAdjustmentApplicationWriter applicationWriter;
    private final RemainSettlementStateService settlementStateService;

    @Transactional(rollbackFor = Exception.class)
    public RemainApplication bind(String adjustmentUuid, RemainAdjustmentNextSettleDTO request) {
        RemainApplication replay = applicationMapper.selectOne(new LambdaQueryWrapper<RemainApplication>()
                .eq(RemainApplication::getRequestId, request.getRequestId()));
        if (replay != null) {
            return replay;
        }
        RemainAdjustment adjustment = requirePending(adjustmentUuid);
        RemainRegistration registration = registrationMapper.selectById(adjustment.getRegistrationUuid());
        SettleOrder target = settleOrderMapper.selectById(request.getSettleUuid());
        if (registration == null || target == null) {
            throw new BusinessException("调整来源登记单或目标结算单不存在");
        }
        lock(adjustment, registration, target, request);
        adjustment = requirePending(adjustmentUuid);
        registration = registrationMapper.selectById(adjustment.getRegistrationUuid());
        target = settleOrderMapper.selectById(request.getSettleUuid());
        validateTarget(adjustment, registration, target);
        List<RemainAdjustmentLine> lines = sourceLines(adjustmentUuid);
        validateAllocation(adjustment, lines);
        RemainApplication application = applicationWriter.write(adjustment, registration, target, lines,
                request.getRequestId());
        adjustment.setTargetType("NEXT_SETTLEMENT");
        adjustment.setTargetSettleUuid(target.getUuid());
        adjustment.setStatus("APPLIED");
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.updateById(adjustment));
        settlementStateService.refresh(target);
        return application;
    }

    private RemainAdjustment requirePending(String adjustmentUuid) {
        RemainAdjustment adjustment = adjustmentMapper.selectById(adjustmentUuid);
        if (adjustment == null || !"PENDING".equals(adjustment.getStatus())
                || !"PENDING".equals(adjustment.getTargetType())) {
            throw new BusinessException("余料结算调整不存在或已处理");
        }
        return adjustment;
    }

    private void lock(RemainAdjustment adjustment, RemainRegistration registration,
                      SettleOrder target, RemainAdjustmentNextSettleDTO request) {
        businessLockService.lockSettleOrders(settlements(adjustment.getSourceSettleUuid(), request.getSettleUuid()));
        businessLockService.lockProcessOrders(List.of(registration.getOrderUuid()));
        remainLockService.lockRegistration(registration.getUuid());
        remainLockService.lockAdjustment(adjustment.getUuid());
    }

    private List<String> settlements(String sourceSettleUuid, String targetSettleUuid) {
        List<String> result = new ArrayList<>();
        if (sourceSettleUuid != null && !sourceSettleUuid.isBlank()) {
            result.add(sourceSettleUuid);
        }
        result.add(targetSettleUuid);
        return result;
    }

    private void validateTarget(RemainAdjustment adjustment, RemainRegistration registration, SettleOrder target) {
        if (registration == null || target == null) {
            throw new BusinessException("调整来源登记单或目标结算单不存在");
        }
        if (target.getUuid().equals(adjustment.getSourceSettleUuid())) {
            throw new BusinessException("待调整余额不能再次挂接来源结算单");
        }
        targetValidator.requireApplicable(target, registration);
        if (value(target.getUnreceivedAmount()).compareTo(adjustment.getAmount()) < 0) {
            throw new BusinessException("目标结算单未收金额不足，不能承接本次待调整余额");
        }
    }

    private List<RemainAdjustmentLine> sourceLines(String adjustmentUuid) {
        return adjustmentLineMapper.selectList(new LambdaQueryWrapper<RemainAdjustmentLine>()
                .eq(RemainAdjustmentLine::getAdjustmentUuid, adjustmentUuid));
    }

    private void validateAllocation(RemainAdjustment adjustment, List<RemainAdjustmentLine> lines) {
        BigDecimal amount = lines.stream().map(RemainAdjustmentLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weight = lines.stream().map(RemainAdjustmentLine::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lines.isEmpty() || amount.compareTo(adjustment.getAmount()) != 0
                || weight.compareTo(adjustment.getWeight()) != 0) {
            throw new BusinessException("待调整余额的来源金额或重量不完整");
        }
    }

    private BigDecimal value(BigDecimal source) {
        return source == null ? BigDecimal.ZERO : source;
    }
}
