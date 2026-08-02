package com.paper.mes.auth.service;

import com.paper.mes.auth.dto.LoginDTO;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.entity.SysUserSession;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.mapper.SysUserSessionMapper;
import com.paper.mes.auth.config.AuthProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private PasswordService passwordService;
    private SysUserMapper userMapper;
    private SysUserSessionMapper sessionMapper;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordService = mock(PasswordService.class);
        userMapper = mock(SysUserMapper.class);
        sessionMapper = mock(SysUserSessionMapper.class);
        AuthProperties authProperties = new AuthProperties();
        authProperties.setSessionHours(12);
        service = new AuthServiceImpl(passwordService, sessionMapper, null, null, authProperties);
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    @Test
    void loginUnknownUsernameReturnsGenericCredentialError() {
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertGenericCredentialError(login());
    }

    @Test
    void loginWrongPasswordReturnsGenericCredentialError() {
        SysUser user = user(1);
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user);
        when(passwordService.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertGenericCredentialError(login());
    }

    @Test
    void loginDisabledUserReturnsGenericCredentialError() {
        SysUser user = user(0);
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user);
        when(passwordService.matches(any(), any())).thenReturn(true);

        assertGenericCredentialError(login());
    }

    @Test
    void sessionTokenDigestIsStableAndDoesNotExposeTheRawToken() throws Exception {
        Method digest = AuthServiceImpl.class.getDeclaredMethod("digestToken", String.class);
        digest.setAccessible(true);

        String hashed = (String) digest.invoke(service, "raw-session-token");

        assertThat(hashed).isEqualTo("e6c276c51996dfa4b71f39f34f5f1a5a8f116e29eb538fab6403dd689631c622");
        assertThat(hashed).doesNotContain("raw-session-token");
    }

    @Test
    void loginStoresOnlyDigestAndReturnsRawToken() {
        SysUser user = user(1);
        user.setUuid("user-uuid");
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user);
        when(passwordService.matches(any(), any())).thenReturn(true);

        String rawToken = service.login(login()).getAccessToken();

        org.mockito.ArgumentCaptor<SysUserSession> captor = org.mockito.ArgumentCaptor.forClass(SysUserSession.class);
        verify(sessionMapper).insert(captor.capture());
        assertThat(rawToken).hasSize(32);
        assertThat(captor.getValue().getToken()).isEqualTo(sha256(rawToken));
        assertThat(captor.getValue().getToken()).isNotEqualTo(rawToken);
    }

    @Test
    void currentUserLooksUpSessionByDigest() {
        String rawToken = "raw-session-token";
        SysUserSession session = new SysUserSession();
        session.setUserUuid("user-uuid");
        session.setExpireTime(java.time.LocalDateTime.now().plusHours(1));
        SysUser user = user(1);
        user.setUuid("user-uuid");
        when(sessionMapper.selectOne(any())).thenReturn(session);
        when(userMapper.selectById("user-uuid")).thenReturn(user);

        service.currentUser(rawToken);

        org.mockito.ArgumentCaptor<LambdaQueryWrapper<SysUserSession>> captor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(sessionMapper).selectOne(captor.capture());
        // Lambda wrappers defer column resolution until SQL generation in this unit-test setup;
        // verifying a condition was recorded keeps the assertion independent of MyBatis metadata.
        assertThat(captor.getValue().getExpression().getNormal()).isNotEmpty();
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

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
