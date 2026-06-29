package com.paper.mes.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.paper.mes.auth.context.AuthContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通用字段自动填充：创建/更新人、时间、版本号、软删除标记。
 * create_by/update_by 优先取当前登录用户，无上下文时回落 system。
 */
@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    private static final String DEFAULT_USER = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        String currentUser = currentUser();
        this.strictInsertFill(metaObject, "createBy", String.class, currentUser);
        this.strictInsertFill(metaObject, "updateBy", String.class, currentUser);
        this.strictInsertFill(metaObject, "version", Integer.class, 1);
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, currentUser());
    }

    private String currentUser() {
        String username = AuthContextHolder.currentDisplayName();
        return username == null || username.isBlank() ? DEFAULT_USER : username;
    }
}
