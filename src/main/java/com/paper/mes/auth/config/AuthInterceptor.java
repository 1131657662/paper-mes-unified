package com.paper.mes.auth.config;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.permission.PublicEndpoint;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements AsyncHandlerInterceptor {

    private static final String CSRF_HEADER = "X-Requested-With";
    private static final String CSRF_HEADER_VALUE = "XMLHttpRequest";
    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (isPublicEndpoint(handler)) return true;
        requireCookieRequestHeader(request);
        String token = authService.resolveToken(request);
        if (token == null) {
            return AuthenticationFailureResponseWriter.writeUnauthorized(response, "请先登录");
        }
        try {
            AuthContextHolder.setCurrentUser(authService.currentUser(token));
        } catch (BusinessException exception) {
            if (exception.getCode() != ResultCode.UNAUTHORIZED) throw exception;
            return AuthenticationFailureResponseWriter.writeUnauthorized(response, exception.getMessage());
        }
        return true;
    }

    private boolean isPublicEndpoint(Object handler) {
        if (!(handler instanceof HandlerMethod method)) return false;
        return method.hasMethodAnnotation(PublicEndpoint.class)
                || method.getBeanType().isAnnotationPresent(PublicEndpoint.class);
    }

    private void requireCookieRequestHeader(HttpServletRequest request) {
        if (!isUnsafeMethod(request.getMethod()) || !authService.isCookieAuthentication(request)) return;
        if (!CSRF_HEADER_VALUE.equals(request.getHeader(CSRF_HEADER))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请求来源校验失败");
        }
    }

    private boolean isUnsafeMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                               Object handler) {
        AuthContextHolder.clear();
    }
}
