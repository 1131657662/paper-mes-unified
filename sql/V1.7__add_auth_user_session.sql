-- 用户登录与会话表：用于前端登录、顶部用户区、后续审计/权限接入。
CREATE TABLE IF NOT EXISTS `sys_user` (
  `uuid`            VARCHAR(36)  NOT NULL COMMENT '用户主键',
  `username`        VARCHAR(50)  NOT NULL COMMENT '登录名',
  `password_hash`   VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
  `real_name`       VARCHAR(50)  NOT NULL COMMENT '姓名',
  `role_code`       VARCHAR(30)  NOT NULL COMMENT '角色编码 admin/operator/finance/warehouse',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 2停用',
  `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `remark`          VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
  `create_by`       VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
  `update_by`       VARCHAR(50)  DEFAULT NULL COMMENT '更新人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `ext_str1`        VARCHAR(255) DEFAULT NULL COMMENT '扩展文本1',
  `ext_str2`        VARCHAR(255) DEFAULT NULL COMMENT '扩展文本2',
  `ext_num1`        DECIMAL(12,3) DEFAULT NULL COMMENT '扩展数值1',
  `ext_num2`        DECIMAL(12,3) DEFAULT NULL COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_role` (`role_code`),
  KEY `idx_sys_user_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS `sys_user_session` (
  `uuid`         VARCHAR(36) NOT NULL COMMENT '会话主键',
  `token`        VARCHAR(64) NOT NULL COMMENT '访问令牌',
  `user_uuid`    VARCHAR(36) NOT NULL COMMENT '用户主键',
  `expire_time`  DATETIME    NOT NULL COMMENT '过期时间',
  `revoked_time` DATETIME    DEFAULT NULL COMMENT '退出/作废时间',
  `is_deleted`   TINYINT     NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
  `create_by`    VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by`    VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`      INT         NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `ext_str1`     VARCHAR(255) DEFAULT NULL COMMENT '扩展文本1',
  `ext_str2`     VARCHAR(255) DEFAULT NULL COMMENT '扩展文本2',
  `ext_num1`     DECIMAL(12,3) DEFAULT NULL COMMENT '扩展数值1',
  `ext_num2`     DECIMAL(12,3) DEFAULT NULL COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uk_sys_user_session_token` (`token`),
  KEY `idx_sys_user_session_user` (`user_uuid`),
  KEY `idx_sys_user_session_expire` (`expire_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户会话表';

-- 默认账号由后端启动初始化器写入，避免在 SQL 中固定 BCrypt salt 后难以轮换。
