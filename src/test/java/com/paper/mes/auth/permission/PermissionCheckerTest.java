package com.paper.mes.auth.permission;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionCheckerTest {
    private final PermissionChecker checker = new PermissionChecker();

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void has_withOwnedPermission_returnsTrue() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().roleCode(RoleCodes.FINANCE).build());

        assertThat(checker.has(Permissions.SETTLE_VIEW)).isTrue();
    }

    @Test
    void has_withRevokedPermission_returnsFalse() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().roleCode(RoleCodes.WAREHOUSE).build());

        assertThat(checker.has(Permissions.SETTLE_VIEW)).isFalse();
    }

    @Test
    void has_withoutAuthenticatedUser_returnsFalse() {
        assertThat(checker.has(Permissions.SETTLE_VIEW)).isFalse();
    }

    @Test
    void hasRolePermission_withOwnedPermission_returnsTrue() {
        assertThat(checker.hasRolePermission(RoleCodes.FINANCE, Permissions.SETTLE_VIEW)).isTrue();
    }

    @Test
    void hasRolePermission_withUnownedPermission_returnsFalse() {
        assertThat(checker.hasRolePermission(RoleCodes.WAREHOUSE, Permissions.SETTLE_VIEW)).isFalse();
    }
}
