package com.paper.mes.common;

/**
 * 业务错误码（V4.1 文档 5.7）。区别于 HTTP 语义的 {@link ResultCode}，
 * 此处为业务语义分类码，随响应体 errorCode 字段下发，便于前端按码分支。
 * 每个码自带默认中文文案，抛异常时可传业务文案覆盖。
 */
public enum ErrorCode {

    /** 状态错误：非法状态或非法状态跳转 */
    E001("E001", "状态不允许该操作"),
    /** 数据不存在 */
    E002("E002", "数据不存在"),
    /** 业务规则冲突 */
    E003("E003", "业务规则冲突"),
    /** 数据已锁定，不可修改 */
    E004("E004", "数据已锁定，不可修改"),
    /** 权重超差拦截 */
    E005("E005", "重量偏差超差，需授权放行"),
    /** 并发冲突：数据已被他人修改 */
    E006("E006", "数据已被他人修改，请刷新后重试");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
