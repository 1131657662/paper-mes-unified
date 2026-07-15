package com.paper.mes.backup.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class BackupOperationGuard {

    private final AtomicReference<String> activeOperation = new AtomicReference<>();

    public boolean acquire(String operation) {
        return activeOperation.compareAndSet(null, operation);
    }

    public void release(String operation) {
        activeOperation.compareAndSet(operation, null);
    }
}
