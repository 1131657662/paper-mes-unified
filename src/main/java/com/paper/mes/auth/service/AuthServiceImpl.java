package com.paper.mes.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.auth.dto.AuthUserVO;
import com.paper.mes.auth.dto.ChangePasswordDTO;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.dto.LoginDTO;
import com.paper.mes.auth.config.AuthProperties;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.entity.SysUserSession;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.mapper.SysUserSessionMapper;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.oplog.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements AuthService {

    private static final int ENABLED = 1;
    private final PasswordService passwordService;
    private final SysUserSessionMapper sessionMapper;
    private final OperationLogService operationLogService;
    private final AuthCookieService cookieService;
    private final AuthProperties authProperties;

    @Override
    @Transactional
    public AuthUserVO login(LoginDTO dto) {
        SysUser user = findByUsername(dto.getUsername());
        if (user == null || !passwordService.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }
        if (!isEnabled(user)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号已停用，请联系管理员");
        }
        String token = createSession(user.getUuid());
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
        return toVO(user, token);
    }

    @Override
    public CurrentUser currentUser(String token) {
        SysUser user = validUserByToken(token);
        return toCurrentUser(user);
    }

    @Override
    public AuthUserVO currentUserVO(String token) {
        SysUser user = validUserByToken(token);
        return toVO(user, token);
    }

    @Override
    @Transactional
    public void changePassword(String token, ChangePasswordDTO dto) {
        SysUser user = validUserByToken(token);
        if (!passwordService.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        if (passwordService.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordService.encode(dto.getNewPassword()));
        updateById(user);
        revokeOtherSessions(user.getUuid(), token);
        operationLogService.record(OperationLogService.BIZ_TYPE_USER, user.getUuid(), user.getUsername(),
                OperationLogService.ACTION_PASSWORD_CHANGE, user.getRealName(), "用户修改自己的登录密码");
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        SysUserSession session = findSession(token);
        if (session == null || session.getRevokedTime() != null) {
            return;
        }
        session.setRevokedTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return cookieService.read(request);
    }

    @Override
    public boolean isCookieAuthentication(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return !StringUtils.hasText(header) && StringUtils.hasText(cookieService.read(request));
    }

    private String createSession(String userUuid) {
        SysUserSession session = new SysUserSession();
        session.setToken(UUID.randomUUID().toString().replace("-", ""));
        session.setUserUuid(userUuid);
        session.setExpireTime(LocalDateTime.now().plusHours(authProperties.getSessionHours()));
        sessionMapper.insert(session);
        return session.getToken();
    }

    private SysUser validUserByToken(String token) {
        SysUserSession session = findSession(token);
        if (session == null || session.getRevokedTime() != null
                || session.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        SysUser user = getById(session.getUserUuid());
        if (user == null || !isEnabled(user)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return user;
    }

    private SysUser findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .last("limit 1"));
    }

    private SysUserSession findSession(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return sessionMapper.selectOne(new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getToken, token)
                .last("limit 1"));
    }

    private void revokeOtherSessions(String userUuid, String currentToken) {
        sessionMapper.update(null, new LambdaUpdateWrapper<SysUserSession>()
                .eq(SysUserSession::getUserUuid, userUuid)
                .isNull(SysUserSession::getRevokedTime)
                .ne(SysUserSession::getToken, currentToken)
                .set(SysUserSession::getRevokedTime, LocalDateTime.now()));
    }

    private AuthUserVO toVO(SysUser user, String token) {
        return AuthUserVO.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(user.getRoleCode())
                .accessToken(token)
                .permissions(Permissions.resolve(user.getRoleCode()))
                .build();
    }

    private CurrentUser toCurrentUser(SysUser user) {
        return CurrentUser.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(user.getRoleCode())
                .build();
    }

    private boolean isEnabled(SysUser user) {
        return user.getStatus() != null && user.getStatus() == ENABLED;
    }
}
