package com.paper.mes.settle.service;

public final class SettleDiscountApprovalStatus {
    public static final int PENDING = 1;
    public static final int APPROVED = 2;
    public static final int USED = 3;
    public static final int REJECTED = 4;
    public static final int CANCELLED = 5;
    public static final int STALE = 6;

    private SettleDiscountApprovalStatus() {
    }
}
