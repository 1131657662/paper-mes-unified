package com.paper.mes.auth.permission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionsTest {

    @Test
    void orderClerk_canCreateAndManageOrdersButCannotBackRecord() {
        var permissions = Permissions.resolve(RoleCodes.ORDER_CLERK);

        assertThat(permissions).contains(Permissions.ORDER_CREATE, Permissions.ORDER_MANAGE);
        assertThat(permissions).doesNotContain(Permissions.ORDER_BACK_RECORD);
    }

    @Test
    void recorder_canBackRecordButCannotCreateOrManageOrders() {
        var permissions = Permissions.resolve(RoleCodes.RECORDER);

        assertThat(permissions).contains(Permissions.ORDER_VIEW, Permissions.ORDER_BACK_RECORD);
        assertThat(permissions).doesNotContain(Permissions.ORDER_CREATE, Permissions.ORDER_MANAGE);
    }

    @Test
    void viewer_hasBusinessViewPermissionsOnly() {
        var permissions = Permissions.resolve(RoleCodes.VIEWER);

        assertThat(permissions).contains(
                Permissions.BASE_VIEW, Permissions.ORDER_VIEW, Permissions.DELIVERY_VIEW,
                Permissions.SETTLE_VIEW, Permissions.REPORT_VIEW);
        assertThat(permissions).noneMatch(permission -> permission.endsWith(":manage"));
        assertThat(permissions).doesNotContain(Permissions.ORDER_CREATE, Permissions.ORDER_BACK_RECORD,
                Permissions.SETTLE_RECEIVE, Permissions.SYSTEM_CONFIG);
    }

    @Test
    void legacyOperator_keepsExistingPermissionSet() {
        assertThat(Permissions.resolve(RoleCodes.OPERATOR)).containsExactly(
                Permissions.BASE_VIEW, Permissions.ORDER_VIEW, Permissions.ORDER_CREATE,
                Permissions.ORDER_BACK_RECORD, Permissions.REPORT_VIEW);
    }
}
