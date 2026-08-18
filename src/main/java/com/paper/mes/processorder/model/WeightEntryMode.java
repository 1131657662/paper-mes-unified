package com.paper.mes.processorder.model;

/** Explicit intent for a source-roll weight submitted by the back-record form. */
public enum WeightEntryMode {
    /** A scale or other physical measurement performed at the worksite. */
    MEASURED,
    /** Copy the stored nominal/reference weight without claiming a measurement. */
    CARRY_NOMINAL,
    /** A deliberate operator estimate that remains provisional for finalization. */
    USER_ESTIMATE,
    /** Explicitly confirm the server-side nominal/reference weight for this submission. */
    CONFIRM_REFERENCE
}
