package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BackRecordReopenService {

    private final OriginalRollMapper rollMapper;

    public void reopen(String orderUuid, String operator) {
        ConcurrencyGuard.requireRowUpdated(rollMapper.update(null,
                new LambdaUpdateWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, orderUuid)
                        .set(OriginalRoll::getIsChecked, 0)
                        .set(OriginalRoll::getCheckUser, null)
                        .set(OriginalRoll::getCheckTime, null)
                        .set(OriginalRoll::getUpdateBy, operator)
                        .set(OriginalRoll::getUpdateTime, LocalDateTime.now())
                        .setSql("version = version + 1")));
    }
}
