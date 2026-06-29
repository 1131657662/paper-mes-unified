package com.paper.mes.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应体：{ "code": 200, "data": {}, "message": "success" }。
 * 失败时可携带业务错误码 errorCode（E001-E005），成功时该字段不输出。
 */
@Data
public class R<T> {

    private int code;
    private T data;
    private String message;

    /** 业务错误码（E001-E005），成功或未分类时为 null，不参与序列化。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    public static <T> R<T> success(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS;
        r.data = data;
        r.message = "success";
        return r;
    }

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> fail(int code, String message) {
        return fail(code, null, message);
    }

    public static <T> R<T> fail(int code, String errorCode, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.data = null;
        r.errorCode = errorCode;
        r.message = message;
        return r;
    }
}
