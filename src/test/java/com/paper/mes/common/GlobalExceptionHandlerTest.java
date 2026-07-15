package com.paper.mes.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void authenticationAndAuthorizationErrorsUseRealHttpStatus() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        assertThat(handler.handleBusiness(new BusinessException(ResultCode.UNAUTHORIZED, "请先登录"))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.FORBIDDEN, "无权限"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.BAD_REQUEST, "参数错误"))
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void missingResourceReturnsHttp404WithUnifiedBusinessBody() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/actuator/env");

        R<Void> response = handler.handleNoResource(exception);
        Method method = GlobalExceptionHandler.class.getMethod("handleNoResource", NoResourceFoundException.class);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);

        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getCode()).isEqualTo(ResultCode.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo("资源不存在");
    }
}
