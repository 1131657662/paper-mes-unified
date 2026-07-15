package com.paper.mes.auth.config;

import com.paper.mes.auth.service.PasswordService;
import com.paper.mes.auth.service.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthBootstrap implements ApplicationRunner {

    private static final int GENERATED_PASSWORD_LENGTH = 16;
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Profiles PROD_PROFILE = Profiles.of("prod");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (schemaBootstrapEnabled()) {
            createTablesIfMissing();
        }
        createDefaultUsersIfMissing();
    }

    private boolean schemaBootstrapEnabled() {
        return !"false".equalsIgnoreCase(environment.getProperty("app.schema-bootstrap.enabled"));
    }

    private void createTablesIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_user` (
                  `uuid` varchar(36) NOT NULL,
                  `username` varchar(50) NOT NULL,
                  `password_hash` varchar(100) NOT NULL,
                  `real_name` varchar(50) NOT NULL,
                  `role_code` varchar(30) NOT NULL,
                  `status` tinyint NOT NULL DEFAULT 1,
                  `last_login_time` datetime DEFAULT NULL,
                  `remark` varchar(255) DEFAULT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1,
                  `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL,
                  `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_sys_user_username` (`username`),
                  KEY `idx_sys_user_role` (`role_code`),
                  KEY `idx_sys_user_status` (`status`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_user_session` (
                  `uuid` varchar(36) NOT NULL,
                  `token` varchar(64) NOT NULL,
                  `user_uuid` varchar(36) NOT NULL,
                  `expire_time` datetime NOT NULL,
                  `revoked_time` datetime DEFAULT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL,
                  `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1,
                  `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL,
                  `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL,
                  PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_sys_user_session_token` (`token`),
                  KEY `idx_sys_user_session_user` (`user_uuid`),
                  KEY `idx_sys_user_session_expire` (`expire_time`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户会话表'
                """);
    }

    private void createDefaultUsersIfMissing() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        insertDefaultUser("u-admin", "admin", initialPassword("app.initial.admin-password", "admin"),
                "系统管理员", "admin");
        insertDefaultUser("u-operator", "operator", initialPassword("app.initial.operator-password", "operator"),
                "录单员", "operator");
    }

    private void insertDefaultUser(String uuid, String username, String password,
                                   String realName, String roleCode) {
        jdbcTemplate.update("""
                INSERT INTO sys_user
                (uuid, username, password_hash, real_name, role_code, status, create_by, update_by)
                VALUES (?, ?, ?, ?, ?, 1, 'system', 'system')
                """, uuid, username, passwordService.encode(password), realName, roleCode);
    }

    private String initialPassword(String propertyName, String username) {
        String configured = environment.getProperty(propertyName);
        if (StringUtils.hasText(configured)) {
            String password = configured.trim();
            if (!PasswordPolicy.isStrong(password)) {
                throw new IllegalStateException(propertyName + " " + PasswordPolicy.MESSAGE);
            }
            return password;
        }
        if (environment.acceptsProfiles(PROD_PROFILE)) {
            throw new IllegalStateException(propertyName + " must be configured in production");
        }
        String generated = generatePassword();
        log.warn("Generated development initial password for user [{}]: {}", username, generated);
        return generated;
    }

    private String generatePassword() {
        String password;
        do {
            password = randomPassword();
        } while (!PasswordPolicy.isStrong(password));
        return password;
    }

    private String randomPassword() {
        StringBuilder builder = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            builder.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return builder.toString();
    }
}
