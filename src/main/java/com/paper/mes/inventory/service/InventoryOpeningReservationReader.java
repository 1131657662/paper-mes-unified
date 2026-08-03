package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryOpeningReservationReader {

    private static final int IN_STOCK = 2;
    private static final int STOCK_LOCK_ACTIVE = 1;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final DeliveryDetailMapper deliveryDetailMapper;

    public Map<String, BigDecimal> read(List<FinishRoll> finishes) {
        if (finishes.isEmpty()) {
            return Map.of();
        }
        Map<String, FinishRoll> finishByUuid = finishes.stream()
                .collect(Collectors.toMap(FinishRoll::getUuid, finish -> finish));
        Map<String, BigDecimal> result = new HashMap<>();
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(new LambdaQueryWrapper<DeliveryDetail>()
                .eq(DeliveryDetail::getStockLockStatus, STOCK_LOCK_ACTIVE));
        for (DeliveryDetail detail : details) {
            FinishRoll finish = finishByUuid.get(detail.getFinishUuid());
            if (finish == null) {
                throw new BusinessException("活跃出库占用关联成品卷不存在");
            }
            BigDecimal reserved = nz(detail.getOutWeight());
            if (reserved.signum() < 0) {
                throw new BusinessException("活跃出库占用重量无效：" + finish.getFinishRollNo());
            }
            if (!Integer.valueOf(IN_STOCK).equals(finish.getFinishStatus()) && reserved.signum() > 0) {
                throw new BusinessException("非在库成品卷存在活跃出库占用：" + finish.getFinishRollNo());
            }
            result.merge(detail.getFinishUuid(), reserved, BigDecimal::add);
        }
        return result;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
