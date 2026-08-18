package com.paper.mes.ai.process.context;

import java.util.List;

/** Read-only draft facts used to assemble a process-parsing request. */
public record ProcessAiOrderContext(
        String orderUuid,
        int draftVersion,
        String remarkLong,
        List<ProcessAiRollContext> rolls,
        ProcessAiReviewBaseline baseline) {

    public ProcessAiOrderContext {
        rolls = List.copyOf(rolls);
    }

    public ProcessAiOrderContext(String orderUuid, int draftVersion, String remarkLong,
                                 List<ProcessAiRollContext> rolls) {
        this(orderUuid, draftVersion, remarkLong, rolls,
                new ProcessAiReviewBaseline(remarkLong, rolls.stream()
                        .map(roll -> new ProcessAiBaselinePlan(
                                roll.shortRef(), roll.originalUuid(), roll.processMode(),
                                roll.mainStepType(), false, null))
                        .toList()));
    }
}
