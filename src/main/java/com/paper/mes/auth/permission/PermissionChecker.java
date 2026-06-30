package com.paper.mes.auth.permission;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PermissionChecker {

    public void require(String... requiredPermissions) {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        List<String> owned = Permissions.resolve(user.getRoleCode());
        if (owned.contains(Permissions.ALL)) {
            return;
        }
        boolean allowed = Arrays.stream(requiredPermissions).anyMatch(owned::contains);
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号没有权限执行该操作");
        }
    }
}
