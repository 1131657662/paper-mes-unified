package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.entity.ProcessOrderAppendRoll;

import java.math.BigDecimal;
import java.util.Objects;

/** Defines which append-roll edits invalidate its saved process plan. */
final class ProcessOrderAppendRollChangePolicy {

    private ProcessOrderAppendRollChangePolicy() {
    }

    static boolean planInvalidated(ProcessOrderAppendRoll current, OriginalRollDTO requested) {
        return sourceChanged(current, requested) || processChanged(current, requested);
    }

    static boolean sourceChanged(ProcessOrderAppendRoll current, OriginalRollDTO requested) {
        int requestedPieces = requested.getPieceNum() == null ? 1 : requested.getPieceNum();
        return !Objects.equals(current.getPaperName(), requested.getPaperName())
                || !Objects.equals(current.getGramWeight(), requested.getGramWeight())
                || !Objects.equals(current.getOriginalWidth(), requested.getOriginalWidth())
                || !Objects.equals(current.getOriginalDiameter(), requested.getOriginalDiameter())
                || !Objects.equals(current.getCoreDiameter(), requested.getCoreDiameter())
                || !Objects.equals(current.getOriginalLength(), requested.getOriginalLength())
                || !Objects.equals(current.getWeightStatus(), requested.getWeightStatus() == null
                ? null : requested.getWeightStatus().name())
                || !sameDecimal(current.getRollWeight(), requested.getRollWeight())
                || !Objects.equals(current.getPieceNum(), requestedPieces);
    }

    static boolean processChanged(ProcessOrderAppendRoll current, OriginalRollDTO requested) {
        int currentMode = normalizeMode(current.getProcessMode());
        int requestedMode = normalizeMode(requested.getProcessMode());
        if (currentMode != requestedMode) return true;
        if (!ProcessModePolicy.requiresMainProcess(requestedMode)) return false;
        return !Objects.equals(current.getMainStepType(), requested.getMainStepType())
                || !Objects.equals(current.getMachineUuid(), requested.getMachineUuid());
    }

    private static int normalizeMode(Integer mode) {
        return mode == null ? ProcessModePolicy.STANDARD : mode;
    }

    private static boolean sameDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) return left == right;
        return left.compareTo(right) == 0;
    }
}
