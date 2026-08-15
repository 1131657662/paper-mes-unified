package com.paper.mes.processorder.model;

/** Supported post-issue disposition commands for an unrecorded source roll. */
public enum ProcessRollDispositionAction {
    DIRECT_SHIP,
    CANCEL,
    SPLIT_TO_ORDER
}
