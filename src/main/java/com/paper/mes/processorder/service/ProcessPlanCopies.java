package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;

final class ProcessPlanCopies {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProcessPlanCopies() {
    }

    static ProcessPlanDTO copy(ProcessPlanDTO value) {
        return MAPPER.convertValue(value, ProcessPlanDTO.class);
    }

    static RewindSegmentPlanDTO copySegment(RewindSegmentPlanDTO value) {
        return MAPPER.convertValue(value, RewindSegmentPlanDTO.class);
    }
}
