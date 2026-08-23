package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProcessRoutePersistenceService {

    private final OriginalRollMapper originalRollMapper;
    private final ProcessRouteCleanupService cleanupService;
    private final ProcessRouteStepWriter stepWriter;
    private final ProcessRouteParamWriter paramWriter;
    private final ProcessRouteFinishWriter finishWriter;
    private final ProcessRouteModePolicy routeModePolicy;

    public ProcessRoutePersistenceService(OriginalRollMapper originalRollMapper,
                                          ProcessRouteCleanupService cleanupService,
                                          ProcessRouteStepWriter stepWriter,
                                          ProcessRouteFinishWriter finishWriter,
                                          ProcessRouteModePolicy routeModePolicy) {
        this(originalRollMapper, cleanupService, stepWriter, null, finishWriter, routeModePolicy);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProcessRoutePersistenceService(OriginalRollMapper originalRollMapper,
                                          ProcessRouteCleanupService cleanupService,
                                          ProcessRouteStepWriter stepWriter,
                                          ProcessRouteParamWriter paramWriter,
                                          ProcessRouteFinishWriter finishWriter,
                                          ProcessRouteModePolicy routeModePolicy) {
        this.originalRollMapper = originalRollMapper;
        this.cleanupService = cleanupService;
        this.stepWriter = stepWriter;
        this.paramWriter = paramWriter;
        this.finishWriter = finishWriter;
        this.routeModePolicy = routeModePolicy;
    }

    public void replaceRoute(ProcessRouteContext context, ProcessRoutePreviewDTO dto,
                             ProcessRoutePreviewVO preview) {
        routeModePolicy.requireCompatible(context.roll(), dto);
        cleanupService.clearExistingRoute(context);
        updateRollRoute(context.roll(), firstStage(dto));
        Map<String, ProcessStageOutput> outputsByKey = stepWriter.write(context, dto, preview);
        if (paramWriter != null) paramWriter.write(context, dto, outputsByKey);
        finishWriter.createFinalFinishes(context, preview, outputsByKey);
    }

    private ProcessRoutePreviewDTO.RouteStageDTO firstStage(ProcessRoutePreviewDTO dto) {
        if (dto.getStages() == null || dto.getStages().isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "工艺路线不能为空");
        }
        return dto.getStages().get(0);
    }

    private void updateRollRoute(OriginalRoll roll, ProcessRoutePreviewDTO.RouteStageDTO firstStage) {
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
