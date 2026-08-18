package com.paper.mes.ai.process.context;

import java.math.BigDecimal;

/** Allowlisted process facts for one source-roll reference. */
public record ProcessAiRollContext(
        String shortRef,
        String originalUuid,
        int rowSort,
        String paperName,
        Integer gramWeight,
        Integer originalWidth,
        Integer originalDiameter,
        Integer coreDiameter,
        BigDecimal rollWeight,
        Integer pieceNum,
        Integer processMode,
        Integer mainStepType) {
}
