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
    public static final String ORDER_VIEW = "order:view";
    public static final String REPORT_VIEW = "report:view";
    public static final String SETTLE_MANAGE = "settle:manage";
    public static final String SETTLE_RECEIVE = "settle:receive";
    public static final String SETTLE_VIEW = "settle:view";
    public static final String SYSTEM_AUDIT = "system:audit";
    public static final String USER_MANAGE = "user:manage";

    private Permissions() {
    }

    public static List<String> resolve(String roleCode) {
        if ("admin".equals(roleCode)) {
            return List.of(ALL);
        }
        if ("operator".equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, ORDER_CREATE, ORDER_BACK_RECORD, REPORT_VIEW);
        }
        if ("finance".equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, DELIVERY_VIEW, SETTLE_VIEW, SETTLE_MANAGE, SETTLE_RECEIVE, REPORT_VIEW);
        }
        if ("warehouse".equals(roleCode)) {
            return List.of(BASE_VIEW, ORDER_VIEW, DELIVERY_VIEW, DELIVERY_MANAGE, REPORT_VIEW);
        }
        return List.of();
    }
}
