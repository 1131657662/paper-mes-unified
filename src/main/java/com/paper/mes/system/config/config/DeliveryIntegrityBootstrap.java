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

    private static final String DETAIL_TABLE = "biz_delivery_detail";
    private static final String ACTIVE_FINISH_COLUMN = "finish_uuid_active";
    private static final String ACTIVE_FINISH_INDEX = "uk_biz_delivery_detail_active_finish";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        normalizeDuplicateDeliveryDetails();
        addActiveFinishColumn();
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

    private void addActiveFinishColumn() {
        if (columnExists(DETAIL_TABLE, ACTIVE_FINISH_COLUMN)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_delivery_detail`
                ADD COLUMN `finish_uuid_active` VARCHAR(32)
                GENERATED ALWAYS AS (
                  CASE
                    WHEN `is_deleted` = 0 THEN NULLIF(TRIM(`finish_uuid`), '')
                    ELSE NULL
                  END
                ) STORED COMMENT 'active unique delivery finish roll'
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
