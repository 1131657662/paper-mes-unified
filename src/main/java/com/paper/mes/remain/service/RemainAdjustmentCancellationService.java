package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.remain.dto.RemainAdjustmentCancelDTO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainAdjustmentLine;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainAdjustmentLineMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainAdjustmentCancellationService {

    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainAdjustmentLineMapper adjustmentLineMapper;
    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper registrationLineMapper;
    private final BusinessLockService businessLockService;
    private final RemainLockService remainLockService;

    @Transactional(rollbackFor = Exception.class)
    public RemainAdjustment cancel(String adjustmentUuid, RemainAdjustmentCancelDTO request) {
        RemainAdjustment adjustment = adjustmentMapper.selectById(adjustmentUuid);
        if (adjustment == null || !"PENDING".equals(adjustment.getStatus())
                || !"PENDING".equals(adjustment.getTargetType())) {
            throw new BusinessException("待调整余额不存在或已进入处理流程");
        }
        RemainRegistration registration = registrationMapper.selectById(adjustment.getRegistrationUuid());
        if (registration == null) {
            throw new BusinessException("待调整来源登记单不存在");
        }
        businessLockService.lockProcessOrders(List.of(registration.getOrderUuid()));
        remainLockService.lockRegistration(registration.getUuid());
        remainLockService.lockAdjustment(adjustmentUuid);
        List<RemainAdjustmentLine> lines = adjustmentLineMapper.selectList(new LambdaQueryWrapper<RemainAdjustmentLine>()
                .eq(RemainAdjustmentLine::getAdjustmentUuid, adjustmentUuid));
        releaseLines(lines);
        adjustment.setStatus("CANCELLED");
        adjustment.setReason(request.getReason());
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.updateById(adjustment));
        return adjustment;
    }

    private void releaseLines(List<RemainAdjustmentLine> lines) {
        for (RemainAdjustmentLine source : lines) {
            RemainRegistrationLine line = registrationLineMapper.selectById(source.getRegistrationLineUuid());
            if (line == null) {
                throw new BusinessException("待调整来源明细不存在");
            }
            line.setAppliedAmount(value(line.getAppliedAmount()).subtract(value(source.getAmount())).max(BigDecimal.ZERO));
            line.setAppliedWeight(value(line.getAppliedWeight()).subtract(value(source.getWeight())).max(BigDecimal.ZERO));
            ConcurrencyGuard.requireRowUpdated(registrationLineMapper.updateById(line));
        }
    }

    private BigDecimal value(BigDecimal source) {
        return source == null ? BigDecimal.ZERO : source;
    }
}
