package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RoleCodes;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReportSubscriptionAccessPolicy {

    private final SysUserMapper userMapper;
    private final PermissionChecker permissionChecker;

    public CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user;
    }

    public List<SysUser> eligibleRecipients(CurrentUser actor) {
        if (!RoleCodes.ADMIN.equals(actor.getRoleCode())) {
            return List.of(requireEligible(actor.getUuid()));
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getRealName, SysUser::getUsername))
                .stream().filter(this::hasReportPermission).toList();
    }

    public List<SysUser> resolveRecipients(CurrentUser actor, Set<String> requested) {
        if (!RoleCodes.ADMIN.equals(actor.getRoleCode())) {
            if (requested != null && !requested.isEmpty()
                    && !Set.copyOf(requested).equals(Set.of(actor.getUuid()))) {
                throw new BusinessException(ResultCode.FORBIDDEN, "普通用户只能订阅给自己");
            }
            return List.of(requireEligible(actor.getUuid()));
        }
        Set<String> uniqueIds = requested == null ? Set.of() : new LinkedHashSet<>(requested);
        if (uniqueIds.isEmpty()) throw new BusinessException("请至少选择一名接收人");
        List<SysUser> users = userMapper.selectBatchIds(uniqueIds);
        if (users.size() != uniqueIds.size() || users.stream().anyMatch(user -> !isEligible(user))) {
            throw new BusinessException("接收人不存在、已停用或没有报表权限");
        }
        return users;
    }

    public boolean isEligible(SysUser user) {
        return user != null && Integer.valueOf(1).equals(user.getStatus()) && hasReportPermission(user);
    }

    private SysUser requireEligible(String uuid) {
        SysUser user = userMapper.selectById(uuid);
        if (!isEligible(user)) throw new BusinessException(ResultCode.FORBIDDEN, "当前账号没有报表订阅权限");
        return user;
    }

    private boolean hasReportPermission(SysUser user) {
        return permissionChecker.hasRolePermission(user.getRoleCode(), Permissions.REPORT_VIEW);
    }
}
