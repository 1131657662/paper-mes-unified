package com.paper.mes.auth.permission;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionChecker permissionChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        if (method.hasMethodAnnotation(PublicEndpoint.class)
                || method.getBeanType().isAnnotationPresent(PublicEndpoint.class)) {
            return true;
        }
        RequirePermission annotation = method.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (annotation == null) {
            if (method.hasMethodAnnotation(AuthenticatedEndpoint.class)
                    || method.getBeanType().isAnnotationPresent(AuthenticatedEndpoint.class)) {
                return true;
            }
            throw new BusinessException(ResultCode.FORBIDDEN, "接口权限策略未配置");
        }
        permissionChecker.require(annotation.value());
        return true;
    }
}
