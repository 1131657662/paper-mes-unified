package com.paper.mes.settle.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SettleReceiveStatusResolver {

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_PARTIAL = 2;
    public static final int STATUS_CLEARED = 3;

    private static final int MONEY_SCALE = 2;

    private SettleReceiveStatusResolver() {
    }

    public static State resolve(BigDecimal totalAmount, BigDecimal receivedAmount) {
        BigDecimal total = amount(totalAmount);
        BigDecimal received = amount(receivedAmount);
        BigDecimal unreceived = total.subtract(received).max(BigDecimal.ZERO)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new State(received, unreceived, statusOf(total, received));
    }

    private static int statusOf(BigDecimal total, BigDecimal received) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return STATUS_CLEARED;
        }
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            return STATUS_PENDING;
        }
        if (received.compareTo(total) >= 0) {
            return STATUS_CLEARED;
        }
        return STATUS_PARTIAL;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record State(BigDecimal receivedAmount, BigDecimal unreceivedAmount, int status) {
    }
}
