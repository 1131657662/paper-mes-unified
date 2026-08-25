package com.paper.mes.remain.config;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Blocks direct remain API access while the frontend module is withdrawn. */
@Component
@RequiredArgsConstructor
public class RemainFeatureInterceptor implements HandlerInterceptor {

    private final RemainProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.isEnabled()) return true;
        throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                "REMAIN_MODULE_DISABLED", "余料模块暂未开放");
    }
}
