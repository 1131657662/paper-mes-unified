package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessRouteDraftPreviewService {

    private static final int STATUS_DRAFT = 0;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final ProcessRoutePreviewer routePreviewer;
    private final ProcessRoutePriceResolver priceResolver;

    public ProcessRoutePreviewVO preview(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessOrder order = requireDraft(orderUuid);
        priceResolver.applyDefaultPrices(order, dto);
        OriginalRoll roll = requireOrderRoll(orderUuid, dto.getOriginalUuid());
        return routePreviewer.preview(roll, dto);
    }

    private ProcessOrder requireDraft(String orderUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可预览后续工艺");
        }
        return order;
    }

    private OriginalRoll requireOrderRoll(String orderUuid, String rollUuid) {
        OriginalRoll roll = rollMapper.selectOne(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getUuid, rollUuid)
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .last("LIMIT 1"));
        if (roll == null) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        return roll;
    }
}
