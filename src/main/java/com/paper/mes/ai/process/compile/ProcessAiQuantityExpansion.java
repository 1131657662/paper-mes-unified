package com.paper.mes.ai.process.compile;

import java.util.List;

public record ProcessAiQuantityExpansion(
        String sourceRollRef,
        List<Integer> widthsMm) {

    public ProcessAiQuantityExpansion {
        widthsMm = List.copyOf(widthsMm);
    }

    public int pieceCount() {
        return widthsMm.size();
    }
}
