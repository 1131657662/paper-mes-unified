package com.paper.mes.exporttask.service;

import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.exporttask.entity.ExportTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ExportTaskExecutionAuthorizer {
    private static final int USER_ENABLED = 1;

    private final SysUserMapper userMapper;
    private final PermissionChecker permissionChecker;

    public boolean canExecute(ExportTask task, String requiredPermission) {
        if (task == null || !StringUtils.hasText(task.getRequesterUuid())) {
            return false;
        }
        SysUser user = userMapper.selectById(task.getRequesterUuid());
        return user != null
                && Integer.valueOf(USER_ENABLED).equals(user.getStatus())
                && permissionChecker.hasRolePermission(user.getRoleCode(), requiredPermission);
    }
}
