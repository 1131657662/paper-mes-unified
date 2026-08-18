package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiDiameterRule;

import java.util.List;

record ProcessAiRewindSegmentInput(
        ProcessAiRollContext owner,
        ProcessAiDiameterRule diameterRule,
        int rewindMode,
        Integer targetDiameter,
        int coreDiameter,
        List<Integer> widths,
        List<ProcessAiRollContext> sources) {

    ProcessAiRewindSegmentInput {
        widths = List.copyOf(widths);
        sources = List.copyOf(sources);
    }
}
