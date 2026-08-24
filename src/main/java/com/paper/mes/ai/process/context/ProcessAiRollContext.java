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
        Integer mainStepType,
        BigDecimal actualWeight,
        BigDecimal totalWeight) {

    /** Compatibility constructor for callers that only have legacy source facts. */
    public ProcessAiRollContext(
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
        this(shortRef, originalUuid, rowSort, paperName, gramWeight, originalWidth,
                originalDiameter, coreDiameter, rollWeight, pieceNum, processMode,
                mainStepType, null, null);
    }

    /** Returns the source-row piece count used by per-mother-roll service pricing. */
    public int sourcePieceCount() {
        return pieceNum == null ? 1 : Math.max(pieceNum, 0);
    }

    /**
     * Returns the authoritative total source weight without using a processed-plan preview.
     * Measured and stored totals take precedence over nominal single-roll weight.
     */
    public BigDecimal sourceTotalWeight() {
        if (positive(actualWeight)) return actualWeight;
        if (positive(totalWeight)) return totalWeight;
        if (!positive(rollWeight) || sourcePieceCount() <= 0) return null;
        return rollWeight.multiply(BigDecimal.valueOf(sourcePieceCount()));
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
