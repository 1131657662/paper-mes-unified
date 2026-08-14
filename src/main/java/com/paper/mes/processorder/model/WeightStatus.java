package com.paper.mes.processorder.model;

/**
 * Weight meaning for an original roll. UNKNOWN is not a numeric zero and
 * must never be used as a tonnage billing input.
 */
public enum WeightStatus {
    UNKNOWN,
    ESTIMATED,
    MEASURED
}
