package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishRoll;

public final class FinishRollStatusPolicy {

    private static final int FINISH_STATUS_SCRAPPED = 4;

    private FinishRollStatusPolicy() {
    }

    public static boolean isScrapped(FinishRoll finish) {
        return finish != null && Integer.valueOf(FINISH_STATUS_SCRAPPED).equals(finish.getFinishStatus());
    }
}
