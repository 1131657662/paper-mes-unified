package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RemainSettlementStateService {

    private final ReceiveRecordMapper receiveRecordMapper;
    private final SettleOrderMapper settleOrderMapper;

    public void refresh(SettleOrder settle) {
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal scrap = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        for (ReceiveRecord record : receiveRecordMapper.selectList(new LambdaQueryWrapper<ReceiveRecord>()
                .eq(ReceiveRecord::getSettleUuid, settle.getUuid())
                .eq(ReceiveRecord::getRecordStatus, 1))) {
            received = received.add(nz(record.getReceiveAmount()));
            cash = cash.add(nz(record.getCashAmount()));
            scrap = scrap.add(nz(record.getScrapOffsetAmount()));
            discount = discount.add(nz(record.getDiscountAmount()));
        }
        BigDecimal total = nz(settle.getTotalAmount());
        BigDecimal unreceived = total.subtract(received).max(BigDecimal.ZERO);
        int status = received.signum() == 0 ? 1 : received.compareTo(total) >= 0 ? 3 : 2;
        LambdaUpdateWrapper<SettleOrder> update = new LambdaUpdateWrapper<SettleOrder>()
                .eq(SettleOrder::getUuid, settle.getUuid())
                .set(SettleOrder::getReceivedAmount, received)
                .set(SettleOrder::getCashReceivedAmount, cash)
                .set(SettleOrder::getScrapOffsetAmount, scrap)
                .set(SettleOrder::getDiscountAmount, discount)
                .set(SettleOrder::getUnreceivedAmount, unreceived)
                .set(SettleOrder::getSettleStatus, status)
                .set(SettleOrder::getUpdateTime, LocalDateTime.now())
                .set(SettleOrder::getUpdateBy, AuthContextHolder.currentDisplayName())
                .setSql("version = version + 1");
        ConcurrencyGuard.requireRowUpdated(settleOrderMapper.update(null, update));
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
