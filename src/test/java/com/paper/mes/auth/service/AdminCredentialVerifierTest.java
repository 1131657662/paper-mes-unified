package com.paper.mes.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCredentialVerifierTest {

    private SysUserMapper userMapper;
    private PasswordService passwordService;
    private AdminCredentialVerifier verifier;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        passwordService = mock(PasswordService.class);
        verifier = new AdminCredentialVerifier(userMapper, passwordService);
    }

    @Test
    void verify_withEnabledAdminAndCorrectPassword_returnsRealIdentity() {
        SysUser admin = user("admin", "管理员张三", "admin", 1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);
        when(passwordService.matches("correct-password", "password-hash")).thenReturn(true);

        AdminCredentialVerifier.VerifiedAdmin verified = verifier.verify("admin", "correct-password");

        assertThat(verified.username()).isEqualTo("admin");
        assertThat(verified.displayName()).isEqualTo("管理员张三");
        verify(passwordService).matches("correct-password", "password-hash");
    }

    @Test
    void verify_withNonAdminAccount_rejectsRelease() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(user("operator", "车间人员", "operator", 1));

        assertThatThrownBy(() -> verifier.verify("operator", "any-password"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无放行权限");
    }

    @Test
    void verify_withWrongPassword_rejectsRelease() {
        SysUser admin = user("admin", "管理员", "admin", 1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);
        when(passwordService.matches("wrong-password", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> verifier.verify("admin", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");
    }

    private SysUser user(String username, String realName, String roleCode, int status) {
        SysUser user = new SysUser();
        user.setUuid("user-uuid");
        user.setUsername(username);
        user.setRealName(realName);
        user.setRoleCode(roleCode);
        user.setStatus(status);
        user.setPasswordHash("password-hash");
        return user;
    }
}
