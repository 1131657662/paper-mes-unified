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
@Order(35)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryInventorySchemaBootstrap implements ApplicationRunner {

    private static final String FINISH_TABLE = "biz_finish_roll";
    private static final String DELIVERY_TABLE = "biz_delivery_order";
    private static final String INVENTORY_INDEX = "idx_finish_inventory_filter";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumn(FINISH_TABLE, "stock_in_time", """
                ALTER TABLE `biz_finish_roll`
                ADD COLUMN `stock_in_time` DATETIME DEFAULT NULL
                COMMENT '首次正式入库时间' AFTER `finish_status`
                """);
        addColumn(DELIVERY_TABLE, "warehouse_uuid", """
                ALTER TABLE `biz_delivery_order`
                ADD COLUMN `warehouse_uuid` VARCHAR(36) DEFAULT NULL
                COMMENT '出库仓库' AFTER `customer_name`
                """);
        addColumn(DELIVERY_TABLE, "warehouse_name", """
                ALTER TABLE `biz_delivery_order`
                ADD COLUMN `warehouse_name` VARCHAR(100) DEFAULT NULL
                COMMENT '出库仓库名称快照' AFTER `warehouse_uuid`
                """);
        backfillStockInTime();
        backfillFinishWarehouse();
        backfillDeliveryWarehouse();
        addInventoryIndex();
    }

    private void backfillStockInTime() {
        jdbcTemplate.update("""
                UPDATE biz_finish_roll f
                INNER JOIN biz_process_order o
                  ON o.uuid = f.order_uuid AND o.is_deleted = 0
                SET f.stock_in_time = o.back_record_time
                WHERE f.stock_in_time IS NULL
                  AND f.finish_status = 2
                  AND o.back_record_time IS NOT NULL
                """);
    }

    private void backfillFinishWarehouse() {
        jdbcTemplate.update("""
                UPDATE biz_finish_roll f
                INNER JOIN biz_process_order o
                  ON o.uuid = f.order_uuid AND o.is_deleted = 0
                SET f.warehouse_uuid = o.warehouse_uuid
                WHERE f.warehouse_uuid IS NULL
                  AND o.warehouse_uuid IS NOT NULL
                """);
        jdbcTemplate.update("""
                UPDATE biz_finish_roll f
                INNER JOIN (
                  SELECT MIN(uuid) AS warehouse_uuid
                  FROM sys_warehouse
                  WHERE is_deleted = 0 AND status = 1
                  HAVING COUNT(*) = 1
                ) single_warehouse ON 1 = 1
                SET f.warehouse_uuid = single_warehouse.warehouse_uuid
                WHERE f.warehouse_uuid IS NULL
                  AND f.finish_status = 2
                  AND f.is_deleted = 0
                """);
    }

    private void backfillDeliveryWarehouse() {
        jdbcTemplate.update("""
                UPDATE biz_delivery_order d
                INNER JOIN (
                  SELECT dd.delivery_uuid,
                         MIN(f.warehouse_uuid) AS warehouse_uuid,
                         MAX(w.warehouse_name) AS warehouse_name
                  FROM biz_delivery_detail dd
                  INNER JOIN biz_finish_roll f
                    ON f.uuid = dd.finish_uuid AND f.is_deleted = 0
                  LEFT JOIN sys_warehouse w
                    ON w.uuid = f.warehouse_uuid AND w.is_deleted = 0
                  WHERE dd.is_deleted = 0 AND f.warehouse_uuid IS NOT NULL
                  GROUP BY dd.delivery_uuid
                  HAVING COUNT(DISTINCT f.warehouse_uuid) = 1
                ) inferred ON inferred.delivery_uuid = d.uuid
                SET d.warehouse_uuid = COALESCE(d.warehouse_uuid, inferred.warehouse_uuid),
                    d.warehouse_name = COALESCE(d.warehouse_name, inferred.warehouse_name)
                WHERE d.warehouse_uuid IS NULL OR d.warehouse_name IS NULL
                """);
    }

    private void addInventoryIndex() {
        if (indexExists(FINISH_TABLE, INVENTORY_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_finish_roll`
                ADD INDEX `idx_finish_inventory_filter`
                (`finish_status`, `is_deleted`, `warehouse_uuid`, `stock_in_time`)
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
                WHERE table_schema = DATABASE()
                  AND table_name = ? AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ? AND index_name = ?
                """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
