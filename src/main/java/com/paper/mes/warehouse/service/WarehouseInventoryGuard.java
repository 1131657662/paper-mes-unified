package com.paper.mes.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseInventoryGuard {

    private static final int FINISH_STATUS_IN_STOCK = 2;
    private static final int ROLL_NO_VOID = 3;

    private final FinishRollMapper finishRollMapper;

    public void requireNoActiveInventory(String warehouseUuid) {
        if (!hasActiveInventory(warehouseUuid)) {
            return;
        }
        throw new BusinessException("仓库仍有在库成品，不能删除或停用");
    }

    private boolean hasActiveInventory(String warehouseUuid) {
        return finishRollMapper.selectOne(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getWarehouseUuid, warehouseUuid)
                .eq(FinishRoll::getFinishStatus, FINISH_STATUS_IN_STOCK)
                .and(wrapper -> wrapper.isNull(FinishRoll::getIsSpare)
                        .or().eq(FinishRoll::getIsSpare, 0))
                .and(wrapper -> wrapper.isNull(FinishRoll::getRollNoStatus)
                        .or().ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID))
                .last("LIMIT 1")) != null;
    }
}
