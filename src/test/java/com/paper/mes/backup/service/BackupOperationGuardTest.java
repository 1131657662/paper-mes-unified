package com.paper.mes.backup.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupOperationGuardTest {

    @Test
    void release_withDifferentOperation_keepsCurrentLock() {
        BackupOperationGuard guard = new BackupOperationGuard();
        assertTrue(guard.acquire("BACKUP"));

        guard.release("DELETE");

        assertFalse(guard.acquire("CLEANUP"));
        guard.release("BACKUP");
        assertTrue(guard.acquire("CLEANUP"));
    }
}
