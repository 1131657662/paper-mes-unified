package com.paper.mes.auth.service;

import com.paper.mes.auth.dto.LoginDTO;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private PasswordService passwordService;
    private SysUserMapper userMapper;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordService = mock(PasswordService.class);
        userMapper = mock(SysUserMapper.class);
        service = new AuthServiceImpl(passwordService, null, null, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    @Test
    void loginUnknownUsernameReturnsGenericCredentialError() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertGenericCredentialError(login());
    }

    @Test
    void loginWrongPasswordReturnsGenericCredentialError() {
        SysUser user = user(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordService.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertGenericCredentialError(login());
    }

    @Test
    void loginDisabledUserReturnsGenericCredentialError() {
        SysUser user = user(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordService.matches("wrong-password", user.getPasswordHash())).thenReturn(true);

        assertGenericCredentialError(login());
    }

    private void assertGenericCredentialError(LoginDTO dto) {
        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误")
                .extracting("code")
                .isEqualTo(ResultCode.BAD_REQUEST);
    }

    private LoginDTO login() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("audit-user");
        dto.setPassword("wrong-password");
        return dto;
    }

    private SysUser user(int status) {
        SysUser user = new SysUser();
        user.setUsername("audit-user");
        user.setPasswordHash("stored-hash");
        user.setStatus(status);
        return user;
    }
}
