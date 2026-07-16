package com.paper.mes.auth.controller;

import com.paper.mes.auth.dto.AuthUserVO;
import com.paper.mes.auth.dto.ChangePasswordDTO;
import com.paper.mes.auth.dto.LoginDTO;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.auth.service.AuthCookieService;
import com.paper.mes.auth.service.LoginAttemptGuard;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final LoginAttemptGuard loginAttemptGuard;

    @PostMapping("/login")
    public R<AuthUserVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request,
                              HttpServletResponse response) {
        String clientId = request.getRemoteAddr();
        loginAttemptGuard.ensureAllowed(clientId);
        AuthUserVO user = authenticate(dto, clientId);
        cookieService.write(response, user.getAccessToken());
        return R.success(user);
    }

    private AuthUserVO authenticate(LoginDTO dto, String clientId) {
        try {
            AuthUserVO user = authService.login(dto);
            loginAttemptGuard.recordSuccess(clientId);
            return user;
        } catch (BusinessException ex) {
            loginAttemptGuard.recordFailure(clientId);
            throw ex;
        }
    }

    @GetMapping("/me")
    public R<AuthUserVO> me(HttpServletRequest request) {
        return R.success(authService.currentUserVO(authService.resolveToken(request)));
    }

    @PostMapping("/password")
    public R<Void> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(authService.resolveToken(request), dto);
        return R.success();
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(authService.resolveToken(request));
        cookieService.clear(response);
        return R.success();
    }
}
