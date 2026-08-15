package com.paper.mes.processorder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates the local-development disposition table; production uses V3.66 SQL. */
@Component
@RequiredArgsConstructor
@Order(40)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessRollDispositionSchemaBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'biz_process_roll_disposition'
                """, Integer.class);
        ensureDispositionActionColumn();
        if (count != null && count > 0) {
            ensureTargetFinishUuidsColumn();
            ensureDispositionIndexes();
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE biz_process_roll_disposition (
                  uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                  source_order_uuid VARCHAR(36) NOT NULL,
                  source_roll_uuid VARCHAR(36) NOT NULL,
                  action_type VARCHAR(32) NOT NULL,
                  status VARCHAR(16) NOT NULL,
                  target_order_uuid VARCHAR(36),
                  target_roll_uuid VARCHAR(36),
                  target_finish_uuid VARCHAR(36),
                  target_finish_uuids JSON,
                  request_id VARCHAR(64) NOT NULL,
                  reason VARCHAR(500) NOT NULL,
                  operator VARCHAR(128) NOT NULL,
                  operate_time DATETIME NOT NULL,
                  source_order_version INT,
                  source_roll_version INT,
                  is_deleted TINYINT NOT NULL DEFAULT 0,
                  create_by VARCHAR(128), update_by VARCHAR(128),
                  create_time DATETIME, update_time DATETIME,
                  version INT NOT NULL DEFAULT 0,
                  ext_str1 VARCHAR(255), ext_str2 VARCHAR(255),
                  ext_num1 DECIMAL(20,6), ext_num2 DECIMAL(20,6),
                  UNIQUE KEY uk_process_roll_disposition_source (source_roll_uuid, is_deleted),
                  UNIQUE KEY uk_process_roll_disposition_request (request_id, is_deleted),
                  KEY idx_process_roll_disposition_order (source_order_uuid, operate_time),
                  CONSTRAINT fk_process_roll_disposition_order FOREIGN KEY (source_order_uuid)
                    REFERENCES biz_process_order (uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_process_roll_disposition_roll FOREIGN KEY (source_roll_uuid)
                    REFERENCES biz_original_roll (uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_process_roll_disposition_target_order FOREIGN KEY (target_order_uuid)
                    REFERENCES biz_process_order (uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_process_roll_disposition_target_roll FOREIGN KEY (target_roll_uuid)
                    REFERENCES biz_original_roll (uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_process_roll_disposition_target_finish FOREIGN KEY (target_finish_uuid)
                    REFERENCES biz_finish_roll (uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT chk_process_roll_disposition_action
                    CHECK (action_type IN ('DIRECT_SHIP', 'CANCEL', 'SPLIT_TO_ORDER')),
                  CONSTRAINT chk_process_roll_disposition_status
                    CHECK (status IN ('APPLIED', 'REJECTED')),
                  CONSTRAINT chk_process_roll_disposition_reason
                    CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                 """);
    }

    private void ensureDispositionIndexes() {
        addUniqueIndex("uk_process_roll_disposition_source",
                "ALTER TABLE biz_process_roll_disposition "
                        + "ADD UNIQUE KEY uk_process_roll_disposition_source (source_roll_uuid, is_deleted)");
        addUniqueIndex("uk_process_roll_disposition_request",
                "ALTER TABLE biz_process_roll_disposition "
                        + "ADD UNIQUE KEY uk_process_roll_disposition_request (request_id, is_deleted)");
    }

    private void addUniqueIndex(String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'biz_process_roll_disposition'
                  AND index_name = ?
                """, Integer.class, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureTargetFinishUuidsColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'biz_process_roll_disposition'
                  AND column_name = 'target_finish_uuids'
                """, Integer.class);
        if (count != null && count > 0) return;
        jdbcTemplate.execute("""
                ALTER TABLE biz_process_roll_disposition
                ADD COLUMN target_finish_uuids JSON DEFAULT NULL
                COMMENT '直发生成的全部成品UUID数组'
                AFTER target_finish_uuid
                """);
    }

    private void ensureDispositionActionColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'biz_original_roll'
                  AND column_name = 'disposition_action'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE biz_original_roll
                    ADD COLUMN disposition_action VARCHAR(32) DEFAULT NULL
                    COMMENT '下发后处置动作，与报废状态分离'
                    AFTER roll_status
                    """);
        }
        ensureDispositionIndex();
    }

    private void ensureDispositionIndex() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'biz_original_roll'
                  AND index_name = 'idx_original_roll_disposition'
                """, Integer.class);
        if (count != null && count > 0) return;
        jdbcTemplate.execute("""
                CREATE INDEX idx_original_roll_disposition
                ON biz_original_roll (order_uuid, disposition_action)
                """);
    }
}
