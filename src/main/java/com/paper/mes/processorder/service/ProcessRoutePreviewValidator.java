package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

final class ProcessRoutePreviewValidator {

    private static final BigDecimal WEIGHT_TOLERANCE_KG = new BigDecimal("1.000");

    private ProcessRoutePreviewValidator() {
    }

    static void validateStageWeight(OriginalRoll roll,
                                    ProcessRoutePreviewDTO.RouteStageDTO stage,
                                    Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs = stage.getOutputs() == null
                ? List.of()
                : stage.getOutputs();
        if (outputs.isEmpty()) {
            throw new BusinessException("每道工序至少需要一个阶段产物");
        }
        BigDecimal sourceWeight = stageSourceWeight(roll, stage, outputsByKey);
        BigDecimal outputWeight = outputWeight(outputs);
        if (outputWeight.compareTo(sourceWeight.add(WEIGHT_TOLERANCE_KG)) > 0) {
            throw new BusinessException("阶段产物预估重量不能超过来源重量");
        }
    }

    private static BigDecimal stageSourceWeight(OriginalRoll roll,
                                                ProcessRoutePreviewDTO.RouteStageDTO stage,
                                                Map<String, ProcessRoutePreviewVO.RouteOutputVO> outputsByKey) {
        if (stage.getInputOutputKeys() == null || stage.getInputOutputKeys().isEmpty()) {
            return originalWeight(roll);
        }
        return stage.getInputOutputKeys().stream()
                .map(outputsByKey::get)
                .map(ProcessRoutePreviewValidator::outputEstimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal originalWeight(OriginalRoll roll) {
        if (roll.getActualWeight() != null) {
            return roll.getActualWeight();
        }
        if (roll.getTotalWeight() != null) {
            return roll.getTotalWeight();
        }
        BigDecimal weight = roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight();
        BigDecimal pieces = BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum());
        return weight.multiply(pieces);
    }

    private static BigDecimal outputWeight(List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs) {
        return outputs.stream()
                .map(ProcessRoutePreviewValidator::expandedOutputWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal expandedOutputWeight(ProcessRoutePreviewDTO.RouteOutputDTO output) {
        BigDecimal weight = output.getEstimateWeight() == null ? BigDecimal.ZERO : output.getEstimateWeight();
        return weight.multiply(BigDecimal.valueOf(output.getCount() == null ? 1 : output.getCount()));
    }

    private static BigDecimal outputEstimateWeight(ProcessRoutePreviewVO.RouteOutputVO output) {
        return output == null || output.getEstimateWeight() == null ? BigDecimal.ZERO : output.getEstimateWeight();
    }
}
