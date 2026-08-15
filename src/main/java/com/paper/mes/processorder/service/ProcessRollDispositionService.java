package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessRollDispositionDTO;
import com.paper.mes.processorder.dto.ProcessRollDispositionVO;

/** Handles explicit post-issue source-roll disposition commands. */
public interface ProcessRollDispositionService {
    ProcessRollDispositionVO dispose(String rollUuid, ProcessRollDispositionDTO dto);
}
