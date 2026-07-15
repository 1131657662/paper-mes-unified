package com.paper.mes.auth.service;

import com.paper.mes.auth.dto.AuthUserVO;
import com.paper.mes.auth.dto.ChangePasswordDTO;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.dto.LoginDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthUserVO login(LoginDTO dto);

    CurrentUser currentUser(String token);

    AuthUserVO currentUserVO(String token);

    void changePassword(String token, ChangePasswordDTO dto);

    void logout(String token);

    String resolveToken(HttpServletRequest request);

    boolean isCookieAuthentication(HttpServletRequest request);
}
