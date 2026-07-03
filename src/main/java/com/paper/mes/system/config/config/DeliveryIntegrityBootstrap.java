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
@Order(31)
public class DeliveryIntegrityBootstrap implements ApplicationRunner {

    private static final String FINISH_TABLE = "biz_finish_roll";
    private static final String REMAINING_WEIGHT_COLUMN = "remaining_weight";
    private static final String DETAIL_TABLE = "biz_delivery_detail";
    private static final String STOCK_LOCK_COLUMN = "stock_lock_status";
    private static final String ACTIVE_FINISH_COLUMN = "finish_uuid_active";
    private static final String ACTIVE_FINISH_INDEX = "uk_biz_delivery_detail_active_finish";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addRemainingWeightColumn();
        syncRemainingWeight();
        addStockLockColumn();
        syncStockLockStatus();
        normalizeDuplicateDeliveryDetails();
        rebuildActiveFinishColumn();
        addActiveFinishIndex();
    }

    private void normalizeDuplicateDeliveryDetails() {
        for (String finishUuid : duplicateFinishUuids()) {
            List<String> detailUuids = activeDetailUuids(finishUuid);
            for (int i = 1; i < detailUuids.size(); i++) {
                softDeleteDetail(detailUuids.get(i));
            }
        }
    }

    private List<String> duplicateFinishUuids() {
        return jdbcTemplate.queryForList("""
                SELECT finish_uuid
                FROM biz_delivery_detail
                WHERE is_deleted = 0
                  AND stock_lock_status = 1
                  AND finish_uuid IS NOT NULL
                  AND TRIM(finish_uuid) <> ''
                GROUP BY finish_uuid
                HAVING COUNT(*) > 1
                """, String.class);
    }

    private List<String> activeDetailUuids(String finishUuid) {
        return jdbcTemplate.queryForList("""
                SELECT d.uuid
                FROM biz_delivery_detail d
                LEFT JOIN biz_delivery_order o
                  ON o.uuid = d.delivery_uuid
                 AND o.is_deleted = 0
                WHERE d.is_deleted = 0
                  AND d.stock_lock_status = 1
                  AND d.finish_uuid = ?
                ORDER BY CASE WHEN o.delivery_status = 2 THEN 0 ELSE 1 END,
                         d.create_time,
                         d.uuid
                """, String.class, finishUuid);
    }

    private void softDeleteDetail(String uuid) {
        jdbcTemplate.update("""
                UPDATE biz_delivery_detail
                SET is_deleted = 1,
                    update_time = ?
                WHERE uuid = ?
                """, LocalDateTime.now(), uuid);
    }

    private void addRemainingWeightColumn() {
        if (columnExists(FINISH_TABLE, REMAINING_WEIGHT_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_finish_roll`
                ADD COLUMN `remaining_weight` DECIMAL(10,3) DEFAULT NULL
                COMMENT '剩余可出库重量kg，NULL按actual_weight兼容旧数据'
                """);
    }

    private void syncRemainingWeight() {
        jdbcTemplate.update("""
                UPDATE biz_finish_roll
                SET remaining_weight = CASE
                    WHEN finish_status = 3 THEN 0.000
                    WHEN actual_weight IS NULL THEN 0.000
                    ELSE actual_weight
                END
                WHERE remaining_weight IS NULL
                """);
    }

    private void addStockLockColumn() {
        if (columnExists(DETAIL_TABLE, STOCK_LOCK_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_delivery_detail`
                ADD COLUMN `stock_lock_status` TINYINT NOT NULL DEFAULT 1
                COMMENT '库存占用状态：1待出库占用 0历史明细不占用'
                """);
    }

    private void syncStockLockStatus() {
        jdbcTemplate.update("""
                UPDATE biz_delivery_detail d
                LEFT JOIN biz_delivery_order o
                  ON o.uuid = d.delivery_uuid
                 AND o.is_deleted = 0
                SET d.stock_lock_status = CASE
                    WHEN d.is_deleted = 0 AND o.delivery_status = 1 THEN 1
                    ELSE 0
                END
                """);
    }

    private void rebuildActiveFinishColumn() {
        if (columnExists(DETAIL_TABLE, ACTIVE_FINISH_COLUMN)
                && activeFinishColumnUsesStockLock()) {
            return;
        }
        dropActiveFinishIndex();
        dropActiveFinishColumn();
        jdbcTemplate.execute("""
                ALTER TABLE `biz_delivery_detail`
                ADD COLUMN `finish_uuid_active` VARCHAR(36)
                GENERATED ALWAYS AS (
                  CASE
                    WHEN `is_deleted` = 0 AND `stock_lock_status` = 1 THEN NULLIF(TRIM(`finish_uuid`), '')
                    ELSE NULL
                  END
                ) STORED COMMENT 'active stock lock finish roll'
                """);
    }

    private void addActiveFinishIndex() {
        if (indexExists(DETAIL_TABLE, ACTIVE_FINISH_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_delivery_detail`
                ADD UNIQUE KEY `uk_biz_delivery_detail_active_finish` (`finish_uuid_active`)
                """);
    }

    private void dropActiveFinishColumn() {
        if (!columnExists(DETAIL_TABLE, ACTIVE_FINISH_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_delivery_detail`
                DROP COLUMN `finish_uuid_active`
                """);
    }

    private void dropActiveFinishIndex() {
        if (!indexExists(DETAIL_TABLE, ACTIVE_FINISH_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_delivery_detail`
                DROP INDEX `uk_biz_delivery_detail_active_finish`
                """);
    }

    private boolean activeFinishColumnUsesStockLock() {
        String expression = jdbcTemplate.queryForObject("""
                SELECT generation_expression
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, String.class, DETAIL_TABLE, ACTIVE_FINISH_COLUMN);
        return expression != null && expression.toLowerCase().contains(STOCK_LOCK_COLUMN);
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
