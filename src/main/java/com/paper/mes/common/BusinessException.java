package com.paper.mes.common;

/**
 * 业务异常，由 GlobalExceptionHandler 统一转为 code=400 响应。
 * 可选携带业务错误码 errorCode（E001-E005），随响应体下发。
 */
public class BusinessException extends RuntimeException {

    private final int code;
    /** 业务错误码（E001-E005），可空。 */
    private final String errorCode;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST, null, message);
    }

    public BusinessException(int code, String message) {
        this(code, null, message);
    }

    /** 带业务错误码，使用该码默认文案。 */
    public BusinessException(ErrorCode errorCode) {
        this(ResultCode.BAD_REQUEST, errorCode.getCode(), errorCode.getDefaultMessage());
    }

    /** 带业务错误码，使用自定义文案覆盖默认。 */
    public BusinessException(ErrorCode errorCode, String message) {
        this(ResultCode.BAD_REQUEST, errorCode.getCode(), message);
    }

    public BusinessException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    public int getCode() {
        return code;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
