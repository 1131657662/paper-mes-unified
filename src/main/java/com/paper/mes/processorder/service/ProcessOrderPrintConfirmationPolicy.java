package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessOrder;

public final class ProcessOrderPrintConfirmationPolicy {

    private static final int PRINT_STATUS_CONFIRMED = 1;

    private ProcessOrderPrintConfirmationPolicy() {
    }

    public static boolean isConfirmed(ProcessOrder order) {
        return order != null
                && order.getPrintStatus() != null
                && order.getPrintStatus() == PRINT_STATUS_CONFIRMED
                && order.getPrintCount() != null
                && order.getPrintCount() > 0;
    }
}
