package com.paper.mes.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常兜底，统一输出 R 结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException ex) {
        R<Void> body = R.fail(ex.getCode(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(authStatus(ex.getCode())).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    /**
     * 唯一键冲突（如加工单号并发重复）。Spring 通常翻译为 DuplicateKeyException，
     * 但异常翻译未生效时底层会直抛 SQLIntegrityConstraintViolationException，两者都兜住。
     */
    @ExceptionHandler({DuplicateKeyException.class, SQLIntegrityConstraintViolationException.class})
    public R<Void> handleDuplicateKey(Exception ex) {
        log.warn("唯一键冲突", ex);
        return R.fail(ResultCode.CONFLICT, "单号已存在，请重试");
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public R<Void> handleBadSqlGrammar(BadSqlGrammarException ex) {
        log.error("数据库结构或 SQL 不匹配", ex);
        return R.fail(ResultCode.ERROR, "数据库结构未同步，请完成最新版本部署后重试");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<Void> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("数据库约束冲突", ex);
        return R.fail(ResultCode.CONFLICT, "数据超出系统允许范围，请检查输入后重试");
    }

    /**
     * 非法参数（如越界的状态码）属客户端输入错误，返回 400 而非 500。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return R.fail(ResultCode.BAD_REQUEST, ex.getMessage());
    }

    /** 上传文件超出配置上限（application.yml max-file-size/max-request-size），属客户端错误。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("上传文件超限", ex);
        return R.fail(ResultCode.BAD_REQUEST, "上传文件超出大小限制（单张≤10MB）");
    }

    /** 缺少必填的 @RequestParam。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return R.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + ex.getParameterName());
    }

    /** 路径/查询参数类型不匹配（如 Integer 字段传了字母）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return R.fail(ResultCode.BAD_REQUEST, "参数类型不正确: " + ex.getName());
    }

    /** 请求体缺失或 JSON 解析失败。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误或为空");
    }

    /** 请求方法不被该端点支持。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return R.fail(ResultCode.METHOD_NOT_ALLOWED, "请求方法不被支持: " + ex.getMethod());
    }

    /** 未匹配到任何处理器/静态资源（Spring Boot 3 默认对未知路径抛此异常）。 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResource(NoResourceFoundException ex) {
        return R.fail(ResultCode.NOT_FOUND, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception ex) {
        log.error("未捕获异常", ex);
        return R.fail(ResultCode.ERROR, "服务器内部错误");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private HttpStatus authStatus(int code) {
        if (code == ResultCode.UNAUTHORIZED) return HttpStatus.UNAUTHORIZED;
        if (code == ResultCode.FORBIDDEN) return HttpStatus.FORBIDDEN;
        if (code == ResultCode.TOO_MANY_REQUESTS) return HttpStatus.TOO_MANY_REQUESTS;
        return HttpStatus.OK;
    }
}
