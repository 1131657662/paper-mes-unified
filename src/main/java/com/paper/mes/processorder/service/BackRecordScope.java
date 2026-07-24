package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;

import java.util.List;

/** 一次部分回录允许修改的完整数据边界。 */
public record BackRecordScope(
        List<OriginalRoll> rolls,
        List<FinishRoll> finishes,
        List<ProcessStep> steps,
        List<FinishOriginalRel> relations) {
}
