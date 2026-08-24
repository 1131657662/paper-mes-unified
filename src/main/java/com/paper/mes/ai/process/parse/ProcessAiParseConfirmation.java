package com.paper.mes.ai.process.parse;

import java.time.LocalDateTime;

public record ProcessAiParseConfirmation(
        String applyIdempotencyKey,
        String acceptedFieldPathsJson,
        String planHash,
        Integer nextVersion,
        String confirmedResultJson,
        String confirmedBy,
        LocalDateTime confirmedAt,
        String acknowledgedDefaultIdsJson) {

    public ProcessAiParseConfirmation(String applyIdempotencyKey, String acceptedFieldPathsJson,
                                      String planHash, Integer nextVersion,
                                      String confirmedResultJson, String confirmedBy,
                                      LocalDateTime confirmedAt) {
        this(applyIdempotencyKey, acceptedFieldPathsJson, planHash, nextVersion,
                confirmedResultJson, confirmedBy, confirmedAt, null);
    }

    public static ProcessAiParseConfirmation empty() {
        return new ProcessAiParseConfirmation(null, null, null, null, null, null, null, null);
    }
}
