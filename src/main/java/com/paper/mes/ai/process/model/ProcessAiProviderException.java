package com.paper.mes.ai.process.model;

public class ProcessAiProviderException extends RuntimeException {

    private final String failureCode;
    private final boolean retryable;

    public ProcessAiProviderException(String failureCode, boolean retryable, String message) {
        super(message);
        this.failureCode = failureCode;
        this.retryable = retryable;
    }

    public String failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
