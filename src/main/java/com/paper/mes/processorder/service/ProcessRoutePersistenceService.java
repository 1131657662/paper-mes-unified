package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcessRoutePersistenceService {

    private final OriginalRollMapper originalRollMapper;
    private final ProcessRouteCleanupService cleanupService;
    private final ProcessRouteStepWriter stepWriter;
    private final ProcessRouteFinishWriter finishWriter;

    public void replaceRoute(ProcessRouteContext context, ProcessRoutePreviewDTO dto,
                             ProcessRoutePreviewVO preview) {
        cleanupService.clearExistingRoute(context);
        updateRollRoute(context.roll(), firstStage(dto));
        Map<String, ProcessStageOutput> outputsByKey = stepWriter.write(context, dto, preview);
        finishWriter.createFinalFinishes(context, preview, outputsByKey);
    }

    private ProcessRoutePreviewDTO.RouteStageDTO firstStage(ProcessRoutePreviewDTO dto) {
        if (dto.getStages() == null || dto.getStages().isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "工艺路线不能为空");
        }
        return dto.getStages().get(0);
    }

    private void updateRollRoute(OriginalRoll roll, ProcessRoutePreviewDTO.RouteStageDTO firstStage) {
        roll.setProcessMode(1);
        roll.setMainStepType(firstStage.getStepType());
        roll.setMachineUuid(resolveStageMachine(firstStage));
        ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
    }

    private String resolveStageMachine(ProcessRoutePreviewDTO.RouteStageDTO stage) {
        if (stage.getMachineUuid() != null && !stage.getMachineUuid().isBlank()) {
            return stage.getMachineUuid();
        }
        return stage.getPlan() == null ? null : stage.getPlan().getMachineUuid();
    }
}
