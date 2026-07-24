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
    E006("E006", "数据已被他人修改，请刷新后重试"),
    /** 权重偏差警告：允许继续，但必须说明原因 */
    E007("E007", "重量偏差较大，需填写原因"),
    /** 历史单据快照存在但无法可靠读取，禁止以当前业务数据代替。 */
    E008("E008", "历史单据快照损坏，请联系管理员处理"),
    /** 超过计价免审额度，必须由具备审批权限的账号处理。 */
    E009("E009", "当前计价优惠超过免审额度，请由财务或管理员账号处理"),
    /** 现结加工单存在未结清款项，必须由当前操作人明确授权放行。 */
    E010("E010", "现结加工单存在未结清款项，需要授权放行");

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
