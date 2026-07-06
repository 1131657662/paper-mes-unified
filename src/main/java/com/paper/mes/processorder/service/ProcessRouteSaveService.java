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
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcessRouteSaveService {

    private static final int STATUS_PENDING = 1;
    private static final int IS_REMAIN_YES = 1;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper originalRollMapper;
    private final ProcessRoutePreviewer routePreviewer;
    private final ProcessRoutePersistenceService persistenceService;
    private final ProcessOrderService processOrderService;
    private final ProcessRoutePriceResolver priceResolver;

    public ProcessRoutePreviewVO preview(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessRouteContext context = loadContext(orderUuid, dto.getOriginalUuid());
        priceResolver.applyDefaultPrices(context.order(), dto);
        return routePreviewer.preview(context.roll(), dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessRoutePreviewVO save(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessRouteContext context = loadContext(orderUuid, dto.getOriginalUuid());
        priceResolver.applyDefaultPrices(context.order(), dto);
        ProcessRoutePreviewVO preview = routePreviewer.preview(context.roll(), dto);
        requireFinalOutputs(preview);
        persistenceService.replaceRoute(context, dto, preview);
        processOrderService.calcFee(orderUuid);
        return preview;
    }

    private ProcessRouteContext loadContext(String orderUuid, String originalUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.E001, "工艺路线配置会重建工序和成品号，仅待下发加工单可操作；待回录请使用追加后续链式工艺");
        }
        OriginalRoll roll = originalRollMapper.selectOne(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getUuid, originalUuid)
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .last("LIMIT 1"));
        if (roll == null) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        return new ProcessRouteContext(order, roll);
    }

    private void requireFinalOutputs(ProcessRoutePreviewVO preview) {
        boolean hasFinal = preview.getOutputs() != null
                && preview.getOutputs().stream()
                .anyMatch(this::isDeliverableOutput);
        if (!hasFinal) {
            throw new BusinessException(ErrorCode.E003, "后续工艺至少需要一个最终成品产出");
        }
    }

    private boolean isDeliverableOutput(ProcessRoutePreviewVO.RouteOutputVO output) {
        return !Boolean.TRUE.equals(output.getConsumedByNextStage())
                && (output.getIsRemain() == null || output.getIsRemain() != IS_REMAIN_YES);
    }
}
