package com.paper.mes.backup.service;

import java.time.LocalDateTime;

public record OffsiteBackupStatus(State state, LocalDateTime lastSyncAt, String remoteName) {

    public enum State {
        NOT_CONFIGURED,
        SUCCESS,
        FAILED,
        INVALID
    }

    public static OffsiteBackupStatus notConfigured() {
        return new OffsiteBackupStatus(State.NOT_CONFIGURED, null, null);
    }

    public static OffsiteBackupStatus invalid() {
        return new OffsiteBackupStatus(State.INVALID, null, null);
    }
}
