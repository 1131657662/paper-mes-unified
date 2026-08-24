package com.paper.mes.ai.process.compile;

import java.util.List;

/** The explicit Step 3 disposition that AI confirmation applies to source mother rolls. */
public record ProcessAiRollConfiguration(
        String ownerRollRef,
        List<String> originalUuids,
        int processMode,
        Integer mainStepType) {

    public ProcessAiRollConfiguration {
        originalUuids = List.copyOf(originalUuids);
    }
}
