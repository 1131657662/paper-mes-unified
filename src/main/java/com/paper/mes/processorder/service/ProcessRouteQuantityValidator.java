package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;

import java.util.List;

final class ProcessRouteQuantityValidator {

    static final int MAX_TOTAL_OUTPUTS = 500;

    private ProcessRouteQuantityValidator() {
    }

    static void requireWithinLimit(ProcessRoutePreviewDTO dto) {
        int total = 0;
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : safe(dto.getStages())) {
            for (ProcessRoutePreviewDTO.RouteOutputDTO output : safe(stage.getOutputs())) {
                int count = output.getCount() == null ? 1 : output.getCount();
                requireValidCount(count);
                if (total > MAX_TOTAL_OUTPUTS - count) {
                    throw limitExceeded();
                }
                total += count;
            }
        }
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
