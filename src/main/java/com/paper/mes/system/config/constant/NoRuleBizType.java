package com.paper.mes.system.config.constant;

import java.util.Set;

public final class NoRuleBizType {

    public static final String PROCESS_ORDER = "process_order";
    public static final String DELIVERY_ORDER = "delivery_order";
    public static final String SETTLE_ORDER = "settle_order";
    public static final String FINISH_ROLL = "finish_roll";
    public static final String CUSTOMER = "customer";
    public static final String PAPER = "paper";
    public static final String MACHINE = "machine";
    public static final String WAREHOUSE = "warehouse";

    private static final Set<String> CORE_TYPES = Set.of(
            PROCESS_ORDER,
            DELIVERY_ORDER,
            SETTLE_ORDER,
            FINISH_ROLL,
            CUSTOMER,
            PAPER,
            MACHINE,
            WAREHOUSE);

    private NoRuleBizType() {
    }

    public static boolean isCore(String bizType) {
        return CORE_TYPES.contains(bizType);
    }
}
