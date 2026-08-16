package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.util.List;

/** Chooses the terminal state for an order after all source rolls are terminal. */
public final class BackRecordCompletionPolicy {

    private static final int ROLL_NO_VOID = 3;

    private BackRecordCompletionPolicy() {
    }

    public static boolean shouldVoid(List<OriginalRoll> activeRolls, List<FinishRoll> finishes) {
        return activeRolls.isEmpty() && finishes.stream().noneMatch(BackRecordCompletionPolicy::hasOutput);
    }

    private static boolean hasOutput(FinishRoll finish) {
        return !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus())
                && !FinishRollStatusPolicy.isScrapped(finish)
                && finish.getActualWeight() != null
                && finish.getActualWeight().signum() > 0;
    }
}
