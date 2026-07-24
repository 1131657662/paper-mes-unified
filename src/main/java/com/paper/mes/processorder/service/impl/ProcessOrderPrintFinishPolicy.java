package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.service.FinishRollStatusPolicy;

import java.util.List;

final class ProcessOrderPrintFinishPolicy {

    private static final int ROLL_NO_VOID = 3;

    private ProcessOrderPrintFinishPolicy() {
    }

    static List<FinishRoll> printable(List<FinishRoll> finishes) {
        if (finishes == null) {
            return List.of();
        }
        return finishes.stream()
                .filter(finish -> !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus()))
                .filter(finish -> !FinishRollStatusPolicy.isScrapped(finish))
                .toList();
    }
}
