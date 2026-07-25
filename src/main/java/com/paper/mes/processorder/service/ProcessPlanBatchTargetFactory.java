package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
class ProcessPlanBatchTargetFactory {

    private static final int REWIND_MODE_MULTI_SOURCE = 5;

    private final ObjectMapper objectMapper;

    ProcessPlanDTO create(ProcessPlanDTO template, String rollUuid) {
        ProcessPlanDTO copy = objectMapper.convertValue(template, ProcessPlanDTO.class);
        if (Integer.valueOf(REWIND_MODE_MULTI_SOURCE).equals(copy.getRewindMode())) {
            return copy;
        }
        rebaseFinishSources(copy.getFinishSpecs(), rollUuid);
        rebaseSegmentSources(copy.getSegments(), rollUuid);
        return copy;
    }

    private void rebaseFinishSources(List<FinishConfigSpecDTO> specs, String rollUuid) {
        if (specs == null) {
            return;
        }
        for (FinishConfigSpecDTO spec : specs) {
            FinishConfigSpecDTO.FinishSourceDTO source = new FinishConfigSpecDTO.FinishSourceDTO();
            source.setOriginalUuid(rollUuid);
            source.setShareRatio(BigDecimal.valueOf(100));
            source.setConsumeRatio(BigDecimal.valueOf(100));
            spec.setSources(List.of(source));
        }
    }

    private void rebaseSegmentSources(List<RewindSegmentPlanDTO> segments, String rollUuid) {
        if (segments == null) {
            return;
        }
        for (RewindSegmentPlanDTO segment : segments) {
            RewindSourcePlanDTO source = new RewindSourcePlanDTO();
            source.setOriginalUuid(rollUuid);
            source.setSourceSort(1);
            source.setShareRatio(BigDecimal.valueOf(100));
            source.setConsumeRatio(BigDecimal.valueOf(100));
            segment.setSources(List.of(source));
        }
    }
}
