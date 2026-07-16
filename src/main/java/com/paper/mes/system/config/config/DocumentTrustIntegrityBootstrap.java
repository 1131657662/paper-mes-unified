package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(32)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentTrustIntegrityBootstrap implements ApplicationRunner {

    private static final String DELIVERY_TABLE = "biz_delivery_order";
    private static final String SETTLE_TABLE = "biz_settle_order";
    private static final String RECEIVE_TABLE = "biz_receive_record";
    private static final String REQUEST_INDEX = "uk_receive_settle_request";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addVoidColumns(DELIVERY_TABLE, "delivery_status");
        addVoidColumns(SETTLE_TABLE, "settle_status");
        addReceiveRequestId();
        addReceiveRequestIndex();
    }

    private void addVoidColumns(String tableName, String statusColumn) {
        addColumn(tableName, "void_reason",
                "ALTER TABLE `" + tableName + "` ADD COLUMN `void_reason` VARCHAR(255) "
                        + "DEFAULT NULL COMMENT '作废原因' AFTER `" + statusColumn + "`");
        addColumn(tableName, "void_by",
                "ALTER TABLE `" + tableName + "` ADD COLUMN `void_by` VARCHAR(50) "
                        + "DEFAULT NULL COMMENT '作废操作人' AFTER `void_reason`");
        addColumn(tableName, "void_time",
                "ALTER TABLE `" + tableName + "` ADD COLUMN `void_time` DATETIME "
                        + "DEFAULT NULL COMMENT '作废时间' AFTER `void_by`");
    }

    private void addReceiveRequestId() {
        addColumn(RECEIVE_TABLE, "request_id", """
                ALTER TABLE `biz_receive_record`
                ADD COLUMN `request_id` VARCHAR(64) DEFAULT NULL
                COMMENT '客户端幂等请求号' AFTER `settle_uuid`
                """);
    }

    private void addReceiveRequestIndex() {
        if (indexExists(RECEIVE_TABLE, REQUEST_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_receive_record`
                ADD UNIQUE KEY `uk_receive_settle_request` (`settle_uuid`, `request_id`)
                """);
    }

    private void addColumn(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
