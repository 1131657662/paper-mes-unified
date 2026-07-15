package com.paper.mes.auth.permission;

import java.util.Set;

public final class RoleCodes {

    public static final String ADMIN = "admin";
    public static final String OPERATOR = "operator";
    public static final String ORDER_CLERK = "order_clerk";
    public static final String RECORDER = "recorder";
    public static final String FINANCE = "finance";
    public static final String WAREHOUSE = "warehouse";
    public static final String VIEWER = "viewer";

    public static final Set<String> ALLOWED = Set.of(
            ADMIN, OPERATOR, ORDER_CLERK, RECORDER, FINANCE, WAREHOUSE, VIEWER);

    private RoleCodes() {
    }
}
