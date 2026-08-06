package com.paper.mes.processorder.service;

public enum ProcessOrderPrintStage {
    DRAFT,
    PENDING_ISSUE,
    PENDING_MANUAL_CONFIRM,
    WAITING_BACK_RECORD,
    COMPLETED,
    SETTLED,
    VOIDED,
    UNKNOWN
}
