package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcessRouteAppendService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_TO_RECORD = 3;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper originalRollMapper;
    private final ProcessStepMapper processStepMapper;
    private final ProcessRoutePreviewer routePreviewer;
    private final ProcessRouteStepWriter stepWriter;
    private final ProcessRouteFinishWriter finishWriter;
    private final ProcessRouteExistingOutputResolver outputResolver;
    private final ProcessRouteSourceConsumer sourceConsumer;
    private final ProcessRoutePriceResolver priceResolver;
    private final ProcessOrderService processOrderService;

    public ProcessRoutePreviewVO preview(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessRouteContext context = loadContext(orderUuid, dto.getOriginalUuid());
        requireAppendStages(dto);
        priceResolver.applyDefaultPrices(context.order(), dto);
        Map<String, ProcessStageOutput> sources = outputResolver.resolveForPreview(context, dto);
        return routePreviewer.previewFromExistingOutputs(context.roll(), sources, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessRoutePreviewVO save(String orderUuid, ProcessRoutePreviewDTO dto) {
        ProcessRouteContext context = loadContext(orderUuid, dto.getOriginalUuid());
        requireAppendStages(dto);
        priceResolver.applyDefaultPrices(context.order(), dto);
        Map<String, ProcessStageOutput> sources = outputResolver.resolveForSave(context, dto);
        ProcessRoutePreviewVO preview = routePreviewer.previewFromExistingOutputs(context.roll(), sources, dto);
        requireFinalOutputs(preview);
        Map<String, ProcessStageOutput> outputsByKey = stepWriter.writeAppend(
                context, dto, preview, sources, nextStepSort(context));
        sourceConsumer.consume(sources.values());
        finishWriter.createFinalFinishes(context, preview, outputsByKey);
        processOrderService.calcFee(orderUuid);
        return preview;
    }

    private ProcessRouteContext loadContext(String orderUuid, String originalUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (!canAppend(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E001, "仅待下发或待回录加工单可追加后续链式工艺");
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

    private boolean canAppend(Integer status) {
        return status != null && (status == STATUS_PENDING || status == STATUS_TO_RECORD);
    }

    private void requireAppendStages(ProcessRoutePreviewDTO dto) {
        if (dto.getStages() == null || dto.getStages().isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "追加工艺不能为空");
        }
        Integer stageLevel = dto.getStages().get(0).getStageLevel();
        if (stageLevel == null || stageLevel <= 1) {
            throw new BusinessException(ErrorCode.E003, "追加工艺必须从第二段或更后阶段开始");
        }
    }

    private int nextStepSort(ProcessRouteContext context) {
        ProcessStep latest = processStepMapper.selectOne(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, context.order().getUuid())
                .eq(ProcessStep::getOriginalUuid, context.roll().getUuid())
                .orderByDesc(ProcessStep::getStepSort)
                .last("LIMIT 1"));
        return latest == null || latest.getStepSort() == null ? 1 : latest.getStepSort() + 1;
    }

    private void requireFinalOutputs(ProcessRoutePreviewVO preview) {
        boolean hasFinal = preview.getOutputs() != null
                && preview.getOutputs().stream()
                .anyMatch(output -> !Boolean.TRUE.equals(output.getConsumedByNextStage()));
        if (!hasFinal) {
            throw new BusinessException(ErrorCode.E003, "追加工艺至少需要一个最终成品产出");
        }
    }
}
