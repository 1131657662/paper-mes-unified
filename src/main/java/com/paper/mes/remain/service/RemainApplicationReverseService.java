package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.remain.dto.RemainApplicationReverseDTO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainApplication;
import com.paper.mes.remain.entity.RemainApplicationLine;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainApplicationLineMapper;
import com.paper.mes.remain.mapper.RemainApplicationMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainApplicationReverseService {

    private final RemainApplicationMapper applicationMapper;
    private final RemainApplicationLineMapper applicationLineMapper;
    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper registrationLineMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final ReceiveRecordMapper receiveRecordMapper;
    private final BusinessLockService businessLockService;
    private final RemainLockService remainLockService;
    private final RemainSettlementStateService settlementStateService;

    @Transactional(rollbackFor = Exception.class)
    public RemainApplication reverse(String applicationUuid, RemainApplicationReverseDTO request) {
        RemainApplication replay = applicationMapper.selectOne(new LambdaQueryWrapper<RemainApplication>()
                .eq(RemainApplication::getRequestId, request.getRequestId()));
        if (replay != null) {
            return replay;
        }
        RemainApplication original = applicationMapper.selectById(applicationUuid);
        if (original == null || !"APPLY".equals(original.getApplicationType())
                || !"ACTIVE".equals(original.getStatus())) {
            throw new BusinessException("余料抵扣应用不存在或已反向");
        }
        SettleOrder settle = settleOrderMapper.selectById(original.getSettleUuid());
        RemainRegistration registration = registrationMapper.selectById(original.getRegistrationUuid());
        if (settle == null || registration == null) {
            throw new BusinessException("原抵扣应用来源不存在");
        }
        businessLockService.lockSettleOrder(settle.getUuid());
        businessLockService.lockProcessOrders(List.of(registration.getOrderUuid()));
        remainLockService.lockRegistration(registration.getUuid());
        RemainApplication lockedOriginal = applicationMapper.selectById(applicationUuid);
        if (lockedOriginal == null || !"ACTIVE".equals(lockedOriginal.getStatus())) {
            throw new BusinessException("原抵扣应用已被其他操作反向");
        }
        ReceiveRecord receive = receiveRecordMapper.selectById(lockedOriginal.getReceiveUuid());
        if (receive == null || receive.getRecordStatus() != 1
                || !"REMAIN_OFFSET".equals(receive.getSourceType())) {
            throw new BusinessException("原余料抵扣收款不可反向");
        }
        RemainAdjustment adjustment = adjustmentFor(lockedOriginal);
        RemainApplication reverse = newReverse(lockedOriginal, request);
        lockedOriginal.setStatus("REVERSED");
        ConcurrencyGuard.requireRowUpdated(applicationMapper.updateById(lockedOriginal));
        applicationMapper.insert(reverse);
        List<RemainApplicationLine> sourceLines = applicationLineMapper.selectList(
                new LambdaQueryWrapper<RemainApplicationLine>()
                        .eq(RemainApplicationLine::getApplicationUuid, applicationUuid));
        for (RemainApplicationLine source : sourceLines) {
            RemainRegistrationLine line = registrationLineMapper.selectById(source.getRegistrationLineUuid());
            if (line == null) {
                throw new BusinessException("抵扣来源明细不存在");
            }
            releaseAllocation(line, source, adjustment);
            RemainApplicationLine reverseLine = new RemainApplicationLine();
            reverseLine.setUuid(UUID.randomUUID().toString());
            reverseLine.setApplicationUuid(reverse.getUuid());
            reverseLine.setRegistrationLineUuid(source.getRegistrationLineUuid());
            reverseLine.setAmount(source.getAmount());
            reverseLine.setWeight(source.getWeight());
            applicationLineMapper.insert(reverseLine);
        }
        receive.setRecordStatus(2);
        receive.setCancelTime(java.time.LocalDateTime.now());
        receive.setCancelBy(AuthContextHolder.currentDisplayName());
        receive.setCancelReason(request.getReason());
        ConcurrencyGuard.requireRowUpdated(receiveRecordMapper.updateById(receive));
        restorePendingAdjustment(adjustment);
        settlementStateService.refresh(settle);
        return reverse;
    }

    private RemainAdjustment adjustmentFor(RemainApplication application) {
        if (application.getAdjustmentUuid() == null || application.getAdjustmentUuid().isBlank()) {
            return null;
        }
        remainLockService.lockAdjustment(application.getAdjustmentUuid());
        RemainAdjustment adjustment = adjustmentMapper.selectById(application.getAdjustmentUuid());
        if (adjustment == null || !"APPLIED".equals(adjustment.getStatus())) {
            throw new BusinessException("抵扣应用的待调整余额不存在或状态异常");
        }
        return adjustment;
    }

    private void releaseAllocation(RemainRegistrationLine line, RemainApplicationLine source,
                                   RemainAdjustment adjustment) {
        if (adjustment == null) {
            line.setAppliedAmount(nz(line.getAppliedAmount()).subtract(source.getAmount()).max(BigDecimal.ZERO));
            line.setAppliedWeight(nz(line.getAppliedWeight()).subtract(source.getWeight()).max(BigDecimal.ZERO));
            ConcurrencyGuard.requireRowUpdated(registrationLineMapper.updateById(line));
        }
    }

    private void restorePendingAdjustment(RemainAdjustment adjustment) {
        if (adjustment == null) {
            return;
        }
        LambdaUpdateWrapper<RemainAdjustment> update = new LambdaUpdateWrapper<RemainAdjustment>()
                .eq(RemainAdjustment::getUuid, adjustment.getUuid())
                .eq(RemainAdjustment::getVersion, adjustment.getVersion())
                .set(RemainAdjustment::getTargetType, "PENDING")
                .set(RemainAdjustment::getTargetSettleUuid, null)
                .set(RemainAdjustment::getStatus, "PENDING")
                .setSql("version = version + 1");
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.update(null, update));
    }

    private RemainApplication newReverse(RemainApplication original, RemainApplicationReverseDTO request) {
        RemainApplication result = new RemainApplication();
        result.setUuid(UUID.randomUUID().toString());
        result.setRegistrationUuid(original.getRegistrationUuid());
        result.setSettleUuid(original.getSettleUuid());
        result.setAdjustmentUuid(original.getAdjustmentUuid());
        result.setCustomerUuid(original.getCustomerUuid());
        result.setApplicationType("REVERSE");
        result.setStatus("ACTIVE");
        result.setAmount(original.getAmount());
        result.setWeight(original.getWeight());
        result.setRequestId(request.getRequestId().trim());
        result.setRequestHash(original.getUuid() + "|" + request.getReason());
        result.setReversalOfUuid(original.getUuid());
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setVersion(1);
        result.setIsDeleted(0);
        return result;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
