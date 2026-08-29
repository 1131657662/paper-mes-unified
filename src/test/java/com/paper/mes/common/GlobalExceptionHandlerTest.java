package com.paper.mes.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void businessErrorsUseRealHttpStatus() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        assertThat(handler.handleBusiness(new BusinessException(ResultCode.UNAUTHORIZED, "请先登录"))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.FORBIDDEN, "无权限"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.TOO_MANY_REQUESTS, "too many"))
                .getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.BAD_REQUEST, "参数错误"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.NOT_FOUND, "不存在"))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleBusiness(new BusinessException(ResultCode.CONFLICT, "状态冲突"))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
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

    @Test
    void missingHandlerReturnsHttp404WithUnifiedBusinessBody() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NoHandlerFoundException exception = new NoHandlerFoundException(
                "GET", "/api/removed-export", null);

        R<Void> response = handler.handleNoHandler(exception);
        Method method = GlobalExceptionHandler.class.getMethod("handleNoHandler", NoHandlerFoundException.class);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);

        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void uncaughtErrorsUseHttp500WithoutExposingInternals() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleOther(new IllegalStateException("secret path"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("服务器内部错误");
        assertThat(response.getBody().getMessage()).doesNotContain("secret path");
    }

    @Test
    void validationAndDatabaseHandlersDeclareNonSuccessHttpStatuses() throws Exception {
        assertResponseStatus("handleValidation",
                org.springframework.web.bind.MethodArgumentNotValidException.class, HttpStatus.BAD_REQUEST);
        assertResponseStatus("handleDuplicateKey", Exception.class, HttpStatus.CONFLICT);
        assertResponseStatus("handleMissingParam",
                org.springframework.web.bind.MissingServletRequestParameterException.class, HttpStatus.BAD_REQUEST);
        assertResponseStatus("handleTypeMismatch",
                org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, HttpStatus.BAD_REQUEST);
        assertResponseStatus("handleNotReadable",
                org.springframework.http.converter.HttpMessageNotReadableException.class, HttpStatus.BAD_REQUEST);
        assertResponseStatus("handleMethodNotSupported",
                org.springframework.web.HttpRequestMethodNotSupportedException.class, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void missingTableReturnsServiceUnavailableWithoutExposingSql() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var error = new org.springframework.jdbc.BadSqlGrammarException(
                "query", "select secret", new SQLException("missing", "42S02", 1146));

        var response = handler.handleBadSqlGrammar(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("DB_SCHEMA_NOT_READY");
        assertThat(response.getBody().getMessage()).doesNotContain("select secret");
    }

    @Test
    void ordinarySqlFailureDoesNotClaimSchemaIsOutOfDate() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var error = new org.springframework.jdbc.BadSqlGrammarException(
                "query", "select secret", new SQLException("syntax", "42000", 1064));

        var response = handler.handleBadSqlGrammar(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("SQL_EXECUTION_ERROR");
        assertThat(response.getBody().getMessage()).doesNotContain("数据库结构未同步");
    }

    @Test
    void dataTooLongReturnsStorageErrorInsteadOfInputConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var cause = new SQLException("Data truncation: Data too long for column 'old_value'");
        var error = new org.springframework.dao.DataIntegrityViolationException("insert failed", cause);

        var response = handler.handleDataIntegrity(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATA_STORAGE_LIMIT");
        assertThat(response.getBody().getMessage()).contains("本次修改未保存");
    }

    @Test
    void ordinaryDataIntegrityReturnsHttp409() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleDataIntegrity(
                new org.springframework.dao.DataIntegrityViolationException("constraint failed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.CONFLICT);
    }

    private void assertResponseStatus(String methodName, Class<?> parameterType, HttpStatus expected) throws Exception {
        Method method = GlobalExceptionHandler.class.getMethod(methodName, parameterType);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);

        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(expected);
    }
}
