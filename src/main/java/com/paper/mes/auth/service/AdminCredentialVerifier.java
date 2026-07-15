package com.paper.mes.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminCredentialVerifier {

    private static final int ENABLED = 1;
    private static final String ADMIN_ROLE = "admin";

    private final SysUserMapper userMapper;
    private final PasswordService passwordService;

    public VerifiedAdmin verify(String username, String password) {
        SysUser user = findUser(username);
        if (!isAuthorized(user, password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "管理员账号或密码错误，或账号无放行权限");
        }
        return new VerifiedAdmin(user.getUuid(), user.getUsername(), displayName(user));
    }

    private SysUser findUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .last("limit 1"));
    }

    private boolean isAuthorized(SysUser user, String password) {
        return user != null
                && user.getStatus() != null && user.getStatus() == ENABLED
                && ADMIN_ROLE.equals(user.getRoleCode())
                && StringUtils.hasText(password)
                && passwordService.matches(password, user.getPasswordHash());
    }

    private String displayName(SysUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    public record VerifiedAdmin(String uuid, String username, String displayName) {
    }
}
