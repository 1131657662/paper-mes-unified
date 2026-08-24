package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainAdjustmentLine;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainAdjustmentLineMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.common.ConcurrencyGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainAdjustmentCommandService {
    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainAdjustmentLineMapper lineMapper;
    private final RemainRegistrationLineMapper registrationLineMapper;

    @Transactional(rollbackFor = Exception.class)
    public RemainAdjustment createPending(RemainRegistration registration, String sourceSettleUuid,
                                          BigDecimal amount, BigDecimal weight, List<RemainRegistrationLine> lines,
                                          String sourceRequestId) {
        if (amount == null || amount.signum() <= 0 || weight == null || weight.signum() <= 0) {
            throw new BusinessException("待调整金额和重量必须同时大于零");
        }
        String requestId = RemainRequestFingerprint.adjustmentRequest(sourceRequestId);
        RemainAdjustment replay = adjustmentMapper.selectOne(new LambdaQueryWrapper<RemainAdjustment>()
                .eq(RemainAdjustment::getRequestId, requestId));
        if (replay != null) {
            return replay;
        }
        RemainAdjustment adjustment = newAdjustment(registration, sourceSettleUuid, amount, weight, requestId);
        adjustmentMapper.insert(adjustment);
        RemainApplicationAllocation.AllocationResult allocation = RemainApplicationAllocation.allocate(lines, amount, weight);
        allocation.lines().forEach(item -> {
            if (item.amount().signum() == 0 && item.weight().signum() == 0) {
                return;
            }
            RemainAdjustmentLine line = new RemainAdjustmentLine();
            line.setUuid(UUID.randomUUID().toString());
            line.setAdjustmentUuid(adjustment.getUuid());
            line.setRegistrationLineUuid(item.line().getUuid());
            line.setAmount(item.amount());
            line.setWeight(item.weight());
            line.setCreateTime(LocalDateTime.now());
            lineMapper.insert(line);
            ConcurrencyGuard.requireRowUpdated(registrationLineMapper.updateById(item.line()));
        });
        return adjustment;
    }

    private RemainAdjustment newAdjustment(RemainRegistration registration, String sourceSettleUuid,
                                           BigDecimal amount, BigDecimal weight, String requestId) {
        RemainAdjustment result = new RemainAdjustment();
        result.setUuid(UUID.randomUUID().toString());
        result.setAdjustmentNo("ADJ-" + result.getUuid().replace("-", "").substring(0, 16));
        result.setRequestId(requestId);
        result.setRequestHash(RemainRequestFingerprint.adjustment(registration.getUuid(), sourceSettleUuid,
                amount, weight));
        result.setRegistrationUuid(registration.getUuid());
        result.setSourceSettleUuid(sourceSettleUuid);
        result.setCustomerUuid(registration.getCustomerUuid());
        result.setTargetType("PENDING");
        result.setStatus("PENDING");
        result.setAmount(amount);
        result.setWeight(weight);
        result.setReason("当前结算未收金额不足，等待财务选择后续去向");
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setVersion(1);
        return result;
    }
}
