package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessOrder;

public final class ProcessOrderPrintStageResolver {

    private ProcessOrderPrintStageResolver() {
    }

    public static ProcessOrderPrintStage resolve(ProcessOrder order) {
        if (order == null || order.getOrderStatus() == null) {
            return ProcessOrderPrintStage.UNKNOWN;
        }
        return switch (order.getOrderStatus()) {
            case 0 -> ProcessOrderPrintStage.DRAFT;
            case 1 -> ProcessOrderPrintStage.PENDING_ISSUE;
            case 2 -> ProcessOrderPrintStage.PENDING_MANUAL_CONFIRM;
            case 3 -> ProcessOrderPrintStage.WAITING_BACK_RECORD;
            case 4 -> ProcessOrderPrintStage.COMPLETED;
            case 5 -> ProcessOrderPrintStage.SETTLED;
            case 6 -> ProcessOrderPrintStage.VOIDED;
            default -> ProcessOrderPrintStage.UNKNOWN;
        };
    }
}
