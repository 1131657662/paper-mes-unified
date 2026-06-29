package com.paper.mes.auth.config;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = authService.resolveToken(request);
        if (token == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        AuthContextHolder.setCurrentUser(authService.currentUser(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContextHolder.clear();
    }
}
