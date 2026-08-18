package com.paper.mes.ai.memory;

import java.util.List;

public record ProjectMemorySelection(
        String context,
        List<String> itemIds) {

    public ProjectMemorySelection {
        itemIds = List.copyOf(itemIds);
    }
}
