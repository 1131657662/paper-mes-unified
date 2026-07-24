package com.paper.mes.report.subscription;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.report.subscription.service.ReportSubscriptionAccessPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportSubscriptionAccessPolicyTest {
    private SysUserMapper userMapper;
    private PermissionChecker permissionChecker;
    private ReportSubscriptionAccessPolicy policy;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        permissionChecker = mock(PermissionChecker.class);
        policy = new ReportSubscriptionAccessPolicy(userMapper, permissionChecker);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void resolveRecipients_nonAdminSelectingAnotherUser_isForbidden() {
        CurrentUser actor = actor("user-1", "finance");

        assertThatThrownBy(() -> policy.resolveRecipients(actor, Set.of("user-2")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("普通用户只能订阅给自己");
    }

    @Test
    void resolveRecipients_nonAdminSelf_returnsEligibleUser() {
        CurrentUser actor = actor("user-1", "finance");
        SysUser user = enabledUser("user-1", "finance");
        when(userMapper.selectById("user-1")).thenReturn(user);
        when(permissionChecker.hasRolePermission("finance", "report:view")).thenReturn(true);

        var result = policy.resolveRecipients(actor, Set.of());

        assertThat(result).containsExactly(user);
    }

    private CurrentUser actor(String uuid, String role) {
        return CurrentUser.builder().uuid(uuid).username(uuid).roleCode(role).build();
    }

    private SysUser enabledUser(String uuid, String role) {
        SysUser user = new SysUser();
        user.setUuid(uuid);
        user.setUsername(uuid);
        user.setRoleCode(role);
        user.setStatus(1);
        return user;
    }
}
