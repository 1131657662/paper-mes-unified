package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DraftOrderVersionGuard {

    private final ProcessOrderMapper orderMapper;

    public void assertExpected(ProcessOrder order, Integer expectedVersion) {
        if (expectedVersion == null || !Objects.equals(order.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.E006, "加工单草稿已被其他页面修改，请刷新后重试");
        }
    }

    public void advance(String orderUuid, Integer expectedVersion) {
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<ProcessOrder>()
                .eq(ProcessOrder::getUuid, orderUuid)
                .eq(ProcessOrder::getVersion, expectedVersion)
                .setSql("version = version + 1"));
        ConcurrencyGuard.requireRowUpdated(updated);
    }
}
