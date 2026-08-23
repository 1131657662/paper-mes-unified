package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;

import java.util.List;

final class ProcessRouteQuantityValidator {

    static final int MAX_TOTAL_OUTPUTS = 500;

    private ProcessRouteQuantityValidator() {
    }

    static void requireWithinLimit(ProcessRoutePreviewDTO dto) {
        long total = 0;
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : safe(dto.getStages())) {
            long planCount = planExpansionCount(stage);
            long outputCount = outputCount(stage);
            for (ProcessRoutePreviewDTO.RouteOutputDTO output : safe(stage.getOutputs())) {
                int count = output.getCount() == null ? 1 : output.getCount();
                requireValidCount(count);
            }
            long stageCount = Math.max(planCount, outputCount);
            if (stageCount > MAX_TOTAL_OUTPUTS || total > MAX_TOTAL_OUTPUTS - stageCount) {
                throw limitExceeded();
            }
            total += stageCount;
        }
    }

    private static long planExpansionCount(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getPlan() == null) return 0;
        if (stage.getPlan().getFinishSpecs() != null && !stage.getPlan().getFinishSpecs().isEmpty()) {
            return stage.getPlan().getFinishSpecs().stream()
                    .mapToLong(spec -> Math.max(1, spec.getCount() == null ? 1 : spec.getCount()))
                    .sum();
        }
        if (stage.getPlan().getSegments() == null) return 0;
        long total = 0;
        for (var segment : stage.getPlan().getSegments()) {
            long repeat = Math.max(1, segment.getRepeatCount() == null ? 1 : segment.getRepeatCount());
            for (var item : safe(segment.getLayoutItems())) {
                long quantity = Math.max(1, item.getQuantity() == null ? 1 : item.getQuantity());
                if (repeat > MAX_TOTAL_OUTPUTS / quantity || total > MAX_TOTAL_OUTPUTS - repeat * quantity) {
                    return MAX_TOTAL_OUTPUTS + 1L;
                }
                total += repeat * quantity;
            }
        }
        return total;
    }

    private static long outputCount(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        return safe(stage.getOutputs()).stream()
                .mapToLong(output -> Math.max(1, output.getCount() == null ? 1 : output.getCount()))
                .sum();
    }

    private static void requireValidCount(int count) {
        if (count < 1 || count > MAX_TOTAL_OUTPUTS) {
            throw limitExceeded();
        }
    }

    private static BusinessException limitExceeded() {
        return new BusinessException("工艺路线展开后的阶段产物总数不能超过500");
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
