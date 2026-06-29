package com.paper.mes.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级变更审计：标在 service 更新方法上，由 {@link FieldAuditAspect} 在方法执行前后
 * 对比实体字段差异，逐字段写入 sys_operation_log（action_type=字段修改），业务代码零侵入。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldAudit {

    /** 业务类型，落 sys_operation_log.biz_type，如 "加工单"。 */
    String bizType();

    /** 被审计实体类，切面据此查旧/新快照并反射遍历字段。 */
    Class<?> entity();

    /** 主键值所在的方法参数下标，默认第 0 个。 */
    int idParam() default 0;

    /** 实体上用作 biz_no 的冗余单号字段名，默认 orderNo。 */
    String bizNoField() default "orderNo";
}
