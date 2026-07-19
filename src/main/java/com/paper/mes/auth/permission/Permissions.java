package com.paper.mes.auth.permission;

import java.util.List;

public final class Permissions {

    public static final String ALL = "*";
    public static final String BASE_MANAGE = "base:manage";
    public static final String BASE_VIEW = "base:view";
    public static final String DELIVERY_MANAGE = "delivery:manage";
    public static final String DELIVERY_VIEW = "delivery:view";
    public static final String ORDER_BACK_RECORD = "order:back-record";
    public static final String ORDER_CREATE = "order:create";
    public static final String ORDER_MANAGE = "order:manage";
    public static final String ORDER_PRICING = "order:pricing";
    public static final String ORDER_PRICING_APPROVE = "order:pricing-approve";
    public static final String ORDER_VIEW = "order:view";
    public static final String REPORT_VIEW = "report:view";
    public static final String EXPORT_TASK_VIEW = "export-task:view";
    public static final String SETTLE_MANAGE = "settle:manage";
    public static final String SETTLE_DISCOUNT = "settle:discount";
    public static final String SETTLE_DISCOUNT_APPROVE = "settle:discount-approve";
    public static final String SETTLE_RECEIVE = "settle:receive";
    public static final String SETTLE_VIEW = "settle:view";
    public static final String DATA_BACKUP = "system:data-backup";
    public static final String DATA_HEALTH = "system:data-health";
    public static final String SYSTEM_AUDIT = "system:audit";
    public static final String SYSTEM_CONFIG = "system:config";
    public static final String USER_MANAGE = "user:manage";

    private Permissions() {
    }

    public static List<String> resolve(String roleCode) {
        if (RoleCodes.ADMIN.equals(roleCode)) {
            return List.of(ALL);
        }
        if (RoleCodes.ORDER_CLERK.equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, ORDER_CREATE, ORDER_MANAGE, ORDER_PRICING, REPORT_VIEW,
                    EXPORT_TASK_VIEW);
        }
        if (RoleCodes.RECORDER.equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, ORDER_BACK_RECORD, REPORT_VIEW, EXPORT_TASK_VIEW);
        }
        if (RoleCodes.OPERATOR.equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, ORDER_CREATE, ORDER_BACK_RECORD, REPORT_VIEW, EXPORT_TASK_VIEW);
        }
        if (RoleCodes.FINANCE.equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, DELIVERY_VIEW, SETTLE_VIEW, SETTLE_MANAGE,
                    SETTLE_RECEIVE, SETTLE_DISCOUNT, ORDER_PRICING, ORDER_PRICING_APPROVE, REPORT_VIEW,
                    EXPORT_TASK_VIEW);
        }
        if (RoleCodes.WAREHOUSE.equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, DELIVERY_VIEW, DELIVERY_MANAGE, REPORT_VIEW, EXPORT_TASK_VIEW);
        }
        if (RoleCodes.VIEWER.equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, DELIVERY_VIEW, SETTLE_VIEW, REPORT_VIEW, EXPORT_TASK_VIEW);
        }
        return List.of();
    }
}
