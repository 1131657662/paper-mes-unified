package com.paper.mes.auth.controller;

import com.paper.mes.auth.dto.AuthUserVO;
import com.paper.mes.auth.dto.LoginDTO;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.R;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/login")
    public R<AuthUserVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.success(authService.login(dto));
    }

    @GetMapping("/me")
    public R<AuthUserVO> me(HttpServletRequest request) {
        return R.success(authService.currentUserVO(authService.resolveToken(request)));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        authService.logout(authService.resolveToken(request));
        return R.success();
    }
}
