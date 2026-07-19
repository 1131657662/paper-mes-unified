package com.paper.mes.exporttask.service;

import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExportTaskExecutionAuthorizerTest {
    private SysUserMapper userMapper;
    private PermissionChecker permissionChecker;
    private ExportTaskExecutionAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        permissionChecker = mock(PermissionChecker.class);
        authorizer = new ExportTaskExecutionAuthorizer(userMapper, permissionChecker);
    }

    @Test
    void canExecute_withEnabledAuthorizedRequester_returnsTrue() {
        ExportTask task = task();
        SysUser user = user(1);
        when(userMapper.selectById(task.getRequesterUuid())).thenReturn(user);
        when(permissionChecker.hasRolePermission(user.getRoleCode(), Permissions.REPORT_VIEW)).thenReturn(true);

        boolean result = authorizer.canExecute(task, Permissions.REPORT_VIEW);

        assertThat(result).isTrue();
        verify(permissionChecker).hasRolePermission(user.getRoleCode(), Permissions.REPORT_VIEW);
    }

    @Test
    void canExecute_withDisabledRequester_returnsFalse() {
        ExportTask task = task();
        when(userMapper.selectById(task.getRequesterUuid())).thenReturn(user(0));

        boolean result = authorizer.canExecute(task, Permissions.REPORT_VIEW);

        assertThat(result).isFalse();
        verifyNoInteractions(permissionChecker);
    }

    @Test
    void canExecute_withDeletedRequester_returnsFalse() {
        ExportTask task = task();
        when(userMapper.selectById(task.getRequesterUuid())).thenReturn(null);

        assertThat(authorizer.canExecute(task, Permissions.REPORT_VIEW)).isFalse();
        verifyNoInteractions(permissionChecker);
    }

    private ExportTask task() {
        ExportTask task = new ExportTask();
        task.setRequesterUuid("user-1");
        return task;
    }

    private SysUser user(int status) {
        SysUser user = new SysUser();
        user.setUuid("user-1");
        user.setRoleCode("finance");
        user.setStatus(status);
        return user;
    }
}
