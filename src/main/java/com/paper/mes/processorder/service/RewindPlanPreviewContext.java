package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;

import java.util.Map;

/** Loaded data needed to preview a rewind plan without issuing follow-up queries. */
public record RewindPlanPreviewContext(
        ProcessOrder order,
        OriginalRoll roll,
        Map<String, OriginalRoll> sourceRolls) {
}
