package com.paper.mes.ai.process.context;

import java.util.List;

public record ProcessAiReviewBaseline(
        String remarkLong,
        List<ProcessAiBaselinePlan> plans) {

    public ProcessAiReviewBaseline {
        plans = List.copyOf(plans);
    }
}
