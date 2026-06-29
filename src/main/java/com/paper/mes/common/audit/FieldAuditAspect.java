package com.paper.mes.common.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paper.mes.oplog.service.OperationLogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 字段级变更审计切面：对标注 {@link FieldAudit} 的更新方法，于执行前后各查一次实体快照，
 * 反射逐字段对比差异，将每个变化字段写入 sys_operation_log（action_type=字段修改）。
 * 审计逻辑全程异常隔离，绝不阻断主业务。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FieldAuditAspect {

    /** 不参与审计的字段：BaseEntity 审计/锁/扩展位 + 实体派生字段。 */
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "version", "updateTime", "updateBy", "createTime", "createBy", "isDeleted",
            "extStr1", "extStr2", "extNum1", "extNum2",
            "totalWeight");

    private final OperationLogService operationLogService;
    private final ApplicationContext applicationContext;

    /** 实体 Class → 对应 BaseMapper。 */
    private final Map<Class<?>, BaseMapper<?>> mapperByEntity = new HashMap<>();

    @PostConstruct
    @SuppressWarnings("rawtypes")
    public void initMapperIndex() {
        for (BaseMapper mapper : applicationContext.getBeansOfType(BaseMapper.class).values()) {
            ResolvableType type = ResolvableType.forClass(mapper.getClass())
                    .as(BaseMapper.class);
            Class<?> entity = type.getGeneric(0).resolve();
            if (entity != null) {
                mapperByEntity.putIfAbsent(entity, mapper);
            }
        }
    }

    @Around("@annotation(fieldAudit)")
    public Object around(ProceedingJoinPoint pjp, FieldAudit fieldAudit) throws Throwable {
        Object id = resolveId(pjp, fieldAudit);
        BaseMapper<?> mapper = mapperByEntity.get(fieldAudit.entity());

        Object before = (id != null && mapper != null) ? safeSelect(mapper, id) : null;
        Object result = pjp.proceed();
        if (before == null) {
            return result;
        }
        try {
            Object after = safeSelect(mapper, id);
            if (after != null) {
                writeDiff(fieldAudit, String.valueOf(id), before, after);
            }
        } catch (Exception ex) {
            log.warn("字段审计写日志失败，bizType={}, id={}", fieldAudit.bizType(), id, ex);
        }
        return result;
    }

    private Object resolveId(ProceedingJoinPoint pjp, FieldAudit fieldAudit) {
        Object[] args = pjp.getArgs();
        int idx = fieldAudit.idParam();
        return (idx >= 0 && idx < args.length) ? args[idx] : null;
    }

    private Object safeSelect(BaseMapper<?> mapper, Object id) {
        return ((BaseMapper<Object>) mapper).selectById((java.io.Serializable) id);
    }

    private void writeDiff(FieldAudit fieldAudit, String bizUuid, Object before, Object after) {
        String bizNo = stringFieldValue(after, fieldAudit.bizNoField());
        for (Field f : allFields(fieldAudit.entity())) {
            if (EXCLUDED_FIELDS.contains(f.getName())) {
                continue;
            }
            ReflectionUtils.makeAccessible(f);
            Object oldVal = ReflectionUtils.getField(f, before);
            Object newVal = ReflectionUtils.getField(f, after);
            if (!Objects.equals(oldVal, newVal)) {
                operationLogService.recordField(
                        fieldAudit.bizType(), bizUuid, bizNo, f.getName(),
                        oldVal == null ? null : String.valueOf(oldVal),
                        newVal == null ? null : String.valueOf(newVal),
                        null);
            }
        }
    }

    /** 遍历实体类自身及父类的声明字段（跳过静态/合成字段）。 */
    private Iterable<Field> allFields(Class<?> entity) {
        Map<String, Field> fields = new java.util.LinkedHashMap<>();
        for (Class<?> c = entity; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.isSynthetic() && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    fields.putIfAbsent(f.getName(), f);
                }
            }
        }
        return fields.values();
    }

    private String stringFieldValue(Object target, String fieldName) {
        Field f = ReflectionUtils.findField(target.getClass(), fieldName);
        if (f == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(f);
        Object v = ReflectionUtils.getField(f, target);
        return v == null ? null : String.valueOf(v);
    }
}
