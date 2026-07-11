package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(30)
public class SettleIntegrityBootstrap implements ApplicationRunner {

    private static final String DETAIL_TABLE = "biz_settle_detail";
    private static final String ACTIVE_ORDER_COLUMN = "active_order_uuid";
    private static final String ACTIVE_ORDER_INDEX = "uk_settle_detail_order_active";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        normalizeDuplicateSettleDetails();
        addActiveOrderColumn();
        addActiveOrderIndex();
    }

    private void normalizeDuplicateSettleDetails() {
        for (String orderUuid : duplicateOrderUuids()) {
            List<String> detailUuids = activeDetailUuids(orderUuid);
            for (int i = 1; i < detailUuids.size(); i++) {
                softDeleteDetail(detailUuids.get(i));
            }
        }
    }

    private List<String> duplicateOrderUuids() {
        return jdbcTemplate.queryForList("""
                SELECT order_uuid
                FROM biz_settle_detail
                WHERE is_deleted = 0
                  AND order_uuid IS NOT NULL
                  AND TRIM(order_uuid) <> ''
                GROUP BY order_uuid
                HAVING COUNT(*) > 1
                """, String.class);
    }

    private List<String> activeDetailUuids(String orderUuid) {
        return jdbcTemplate.queryForList("""
                SELECT uuid
                FROM biz_settle_detail
                WHERE is_deleted = 0
                  AND order_uuid = ?
                ORDER BY create_time, uuid
                """, String.class, orderUuid);
    }

    private void softDeleteDetail(String uuid) {
        jdbcTemplate.update("""
                UPDATE biz_settle_detail
                SET is_deleted = 1,
                    update_time = ?
                WHERE uuid = ?
                """, LocalDateTime.now(), uuid);
    }

    private void addActiveOrderColumn() {
        if (columnExists(DETAIL_TABLE, ACTIVE_ORDER_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_detail`
                ADD COLUMN `active_order_uuid` VARCHAR(36)
                GENERATED ALWAYS AS (
                  CASE
                    WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`order_uuid`), '')
                    ELSE NULL
                  END
                ) STORED COMMENT 'active unique settled process order'
                """);
    }

    private void addActiveOrderIndex() {
        if (indexExists(DETAIL_TABLE, ACTIVE_ORDER_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_settle_detail`
                ADD UNIQUE KEY `uk_settle_detail_order_active` (`active_order_uuid`)
                """);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
