package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessRouteCatalogPolicy {

    private static final Set<Integer> SUPPORTED_STEP_TYPES = Set.of(
            FeeCalculator.STEP_TYPE_SAW,
            FeeCalculator.STEP_TYPE_REWIND
    );

    private final ProcessCatalogService catalogService;

    public void validate(List<ProcessRoutePreviewDTO.RouteStageDTO> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "工艺路线不能为空");
        }
        Map<Integer, ProcessCatalogVO> activeByType = catalogService.listActive().stream()
                .collect(Collectors.toMap(ProcessCatalogVO::stepType, Function.identity()));
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : stages) {
            validateStage(stage, activeByType);
        }
    }

    private void validateStage(ProcessRoutePreviewDTO.RouteStageDTO stage,
                               Map<Integer, ProcessCatalogVO> activeByType) {
        ProcessCatalogVO catalog = activeByType.get(stage.getStepType());
        if (catalog == null) {
            throw new BusinessException(ErrorCode.E003, "工序类型未启用或不存在");
        }
        if (!SUPPORTED_STEP_TYPES.contains(catalog.stepType()) || !catalog.producesInventoryOutput()) {
            throw new BusinessException(ErrorCode.E003, "链式工艺仅支持锯纸和复卷，服务工序请单独追加");
        }
        if (isMainStage(stage) && !catalog.allowsMainProcess()) {
            throw new BusinessException(ErrorCode.E003, catalog.name() + "不能作为主工艺");
        }
        if (!catalog.supportsBillingMode(ProcessStepPricingPolicy.STANDARD)) {
            throw new BusinessException(ErrorCode.E003, catalog.name() + "不支持链式工艺标准计费");
        }
    }

    private boolean isMainStage(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        return stage.getStageLevel() != null && stage.getStageLevel() <= 1;
    }
}
