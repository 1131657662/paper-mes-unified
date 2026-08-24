package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.remain.dto.RemainApplicationCreateDTO;
import com.paper.mes.remain.entity.RemainApplication;
import com.paper.mes.remain.entity.RemainApplicationLine;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainApplicationLineMapper;
import com.paper.mes.remain.mapper.RemainApplicationMapper;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainApplicationCommandService {

    private final RemainApplicationMapper applicationMapper;
    private final RemainApplicationLineMapper applicationLineMapper;
    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final ReceiveRecordMapper receiveRecordMapper;
    private final BusinessLockService businessLockService;
    private final RemainLockService remainLockService;
    private final RemainSettlementStateService settlementStateService;
    private final RemainAdjustmentCommandService adjustmentCommandService;
    private final RemainApplicationTargetValidator targetValidator;

    @Transactional(rollbackFor = Exception.class)
    public RemainApplication apply(String registrationUuid, RemainApplicationCreateDTO request) {
        RemainApplication replay = applicationMapper.selectOne(new LambdaQueryWrapper<RemainApplication>()
                .eq(RemainApplication::getRequestId, request.getRequestId()));
        if (replay != null) {
            return replay;
        }
        SettleOrder settle = settleOrderMapper.selectById(request.getSettleUuid());
        RemainRegistration registration = registrationMapper.selectById(registrationUuid);
        if (settle == null || registration == null) {
            throw new BusinessException("结算单或登记单不存在");
        }
        businessLockService.lockSettleOrder(settle.getUuid());
        businessLockService.lockProcessOrders(List.of(registration.getOrderUuid()));
        remainLockService.lockRegistration(registrationUuid);
        settle = settleOrderMapper.selectById(settle.getUuid());
        registration = registrationMapper.selectById(registrationUuid);
        targetValidator.requireApplicable(settle, registration);
        List<RemainRegistrationLine> lines = lines(registrationUuid);
        lockLines(lines);
        lines = lines(registrationUuid);
        BigDecimal remainingAmount = remainingApplicationAmount(lines);
        BigDecimal remainingWeight = lines.stream().map(RemainApplicationAllocation::remainingWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unreceived = nz(settle.getUnreceivedAmount());
        BigDecimal amount = remainingAmount.min(unreceived).setScale(0, RoundingMode.DOWN);
        if (amount.signum() <= 0) {
            throw new BusinessException("当前结算没有可抵扣的未收金额，需进入结算调整");
        }
        BigDecimal weight = amount.compareTo(remainingAmount) == 0 ? remainingWeight
                : remainingWeight.multiply(amount).divide(remainingAmount, 3, RoundingMode.HALF_UP);
        RemainApplication application = newApplication(registration, settle, request, amount, weight);
        applicationMapper.insert(application);
        RemainApplicationAllocation.AllocationResult allocation = RemainApplicationAllocation.allocate(
                lines, amount, weight);
        writeAllocation(application, allocation);
        ReceiveRecord receive = newReceive(application, settle, amount, weight);
        receiveRecordMapper.insert(receive);
        application.setReceiveUuid(receive.getUuid());
        ConcurrencyGuard.requireRowUpdated(applicationMapper.updateById(application));
        createPendingAdjustment(registration, settle, request, lines, remainingAmount, remainingWeight,
                amount, weight);
        settlementStateService.refresh(settle);
        return application;
    }

    private void createPendingAdjustment(RemainRegistration registration, SettleOrder settle,
                                         RemainApplicationCreateDTO request,
                                         List<RemainRegistrationLine> lines,
                                         BigDecimal remainingAmount, BigDecimal remainingWeight,
                                         BigDecimal appliedAmount, BigDecimal appliedWeight) {
        BigDecimal leftoverAmount = remainingAmount.subtract(appliedAmount).setScale(0, RoundingMode.DOWN);
        BigDecimal leftoverWeight = remainingWeight.subtract(appliedWeight).max(BigDecimal.ZERO);
        if (leftoverAmount.signum() <= 0 || leftoverWeight.signum() <= 0) {
            return;
        }
        adjustmentCommandService.createPending(registration, settle.getUuid(), leftoverAmount,
                leftoverWeight, lines, request.getRequestId());
    }

    private List<RemainRegistrationLine> lines(String registrationUuid) {
        return lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid)
                .orderByAsc(RemainRegistrationLine::getUuid));
    }

    private void lockLines(List<RemainRegistrationLine> lines) {
        remainLockService.lockLines(lines.stream().map(RemainRegistrationLine::getUuid).toList());
    }

    private BigDecimal remainingApplicationAmount(List<RemainRegistrationLine> lines) {
        return lines.stream().map(RemainApplicationAllocation::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RemainApplication newApplication(RemainRegistration registration, SettleOrder settle,
                                              RemainApplicationCreateDTO request, BigDecimal amount,
                                              BigDecimal weight) {
        RemainApplication result = new RemainApplication();
        result.setUuid(UUID.randomUUID().toString());
        result.setRegistrationUuid(registration.getUuid());
        result.setSettleUuid(settle.getUuid());
        result.setCustomerUuid(registration.getCustomerUuid());
        result.setApplicationType("APPLY");
        result.setStatus("ACTIVE");
        result.setAmount(amount);
        result.setWeight(weight);
        result.setRequestId(request.getRequestId().trim());
        result.setRequestHash(RemainRequestFingerprint.application(registration.getUuid(), settle.getUuid(),
                amount, weight));
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setVersion(1);
        result.setIsDeleted(0);
        return result;
    }

    private void writeAllocation(RemainApplication application,
                                 RemainApplicationAllocation.AllocationResult allocation) {
        for (RemainApplicationAllocation.LineAllocation item : allocation.lines()) {
            if (item.amount().signum() == 0 && item.weight().signum() == 0) {
                continue;
            }
            RemainApplicationLine line = new RemainApplicationLine();
            line.setUuid(UUID.randomUUID().toString());
            line.setApplicationUuid(application.getUuid());
            line.setRegistrationLineUuid(item.line().getUuid());
            line.setAmount(item.amount());
            line.setWeight(item.weight());
            applicationLineMapper.insert(line);
            ConcurrencyGuard.requireRowUpdated(lineMapper.updateById(item.line()));
        }
    }

    private ReceiveRecord newReceive(RemainApplication application, SettleOrder settle,
                                     BigDecimal amount, BigDecimal weight) {
        ReceiveRecord record = new ReceiveRecord();
        record.setUuid(UUID.randomUUID().toString());
        record.setSettleUuid(settle.getUuid());
        record.setRequestId(application.getRequestId());
        record.setRequestHash(application.getRequestHash());
        record.setReceiveDate(LocalDateTime.now());
        record.setReceiveAmount(amount);
        record.setCashAmount(BigDecimal.ZERO);
        record.setScrapOffsetAmount(amount);
        record.setDiscountAmount(BigDecimal.ZERO);
        record.setScrapWeight(weight);
        record.setScrapUnitPrice(weight.signum() == 0 ? BigDecimal.ZERO
                : amount.divide(weight, 4, RoundingMode.HALF_UP));
        record.setReceiveType(2);
        record.setSourceType("REMAIN_OFFSET");
        record.setRemainApplicationUuid(application.getUuid());
        record.setOperator(AuthContextHolder.currentDisplayName());
        record.setRecordStatus(1);
        record.setIsDeleted(0);
        record.setVersion(1);
        return record;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
