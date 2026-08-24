package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.remain.dto.RemainAdjustmentCreateDTO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainAdjustmentCreationService {

    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainAdjustmentMapper adjustmentMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final SettleDetailMapper settleDetailMapper;
    private final BusinessLockService businessLockService;
    private final RemainLockService remainLockService;
    private final RemainAdjustmentCommandService adjustmentCommandService;

    @Transactional(rollbackFor = Exception.class)
    public RemainAdjustment create(String registrationUuid, RemainAdjustmentCreateDTO request) {
        RemainAdjustment replay = adjustmentMapper.selectOne(new LambdaQueryWrapper<RemainAdjustment>()
                .eq(RemainAdjustment::getRequestId, RemainRequestFingerprint.adjustmentRequest(request.getRequestId())));
        if (replay != null) {
            return replay;
        }
        RemainRegistration registration = registrationMapper.selectById(registrationUuid);
        SettleOrder source = settleOrderMapper.selectById(request.getSourceSettleUuid());
        if (registration == null || source == null) {
            throw new BusinessException("登记单或原结算单不存在");
        }
        businessLockService.lockSettleOrder(source.getUuid());
        businessLockService.lockProcessOrders(List.of(registration.getOrderUuid()));
        remainLockService.lockRegistration(registration.getUuid());
        registration = registrationMapper.selectById(registrationUuid);
        source = settleOrderMapper.selectById(request.getSourceSettleUuid());
        validateSource(registration, source);
        List<RemainRegistrationLine> lines = lines(registrationUuid);
        remainLockService.lockLines(lines.stream().map(RemainRegistrationLine::getUuid).toList());
        lines = lines(registrationUuid);
        BigDecimal amount = lines.stream().map(RemainApplicationAllocation::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weight = lines.stream().map(RemainApplicationAllocation::remainingWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.signum() <= 0 || weight.signum() <= 0) {
            throw new BusinessException("登记单没有可进入待调整的金额或重量");
        }
        return adjustmentCommandService.createPending(registration, source.getUuid(), amount, weight,
                lines, request.getRequestId());
    }

    private void validateSource(RemainRegistration registration, SettleOrder source) {
        if (!"CONFIRMED".equals(registration.getPriceStatus())) {
            throw new BusinessException("登记单尚未确认价格");
        }
        if (!registration.getCustomerUuid().equals(source.getCustomerUuid())) {
            throw new BusinessException("登记客户与原结算客户不一致");
        }
        if (!hasSourceOrder(source, registration)) {
            throw new BusinessException("登记来源加工单不在原结算单内");
        }
        if (source.getSettleStatus() != null && source.getSettleStatus() < 3
                && value(source.getUnreceivedAmount()).signum() > 0) {
            throw new BusinessException("原结算单仍有未收金额，应先在当前结算抵扣");
        }
    }

    private boolean hasSourceOrder(SettleOrder source, RemainRegistration registration) {
        return settleDetailMapper.selectList(new LambdaQueryWrapper<SettleDetail>()
                        .eq(SettleDetail::getSettleUuid, source.getUuid())
                        .eq(SettleDetail::getOrderUuid, registration.getOrderUuid()))
                .stream().findAny().isPresent();
    }

    private List<RemainRegistrationLine> lines(String registrationUuid) {
        return lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid)
                .orderByAsc(RemainRegistrationLine::getUuid));
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
