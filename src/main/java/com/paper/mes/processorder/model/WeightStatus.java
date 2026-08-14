package com.paper.mes.processorder.model;

/**
 * Weight meaning for an original roll. ESTIMATED is a supplier label,
 * historical average, or other reference value that has not been weighed.
 * UNKNOWN is not a numeric zero and must never be used as a tonnage billing input.
 */
public enum WeightStatus {
    UNKNOWN,
    ESTIMATED,
    MEASURED
}
