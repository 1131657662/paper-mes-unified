SET @stock_in_time_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_finish_roll'
    AND column_name = 'stock_in_time'
);
SET @sql := IF(@stock_in_time_missing,
  'ALTER TABLE `biz_finish_roll` ADD COLUMN `stock_in_time` DATETIME DEFAULT NULL COMMENT ''首次正式入库时间'' AFTER `finish_status`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @delivery_warehouse_uuid_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_delivery_order'
    AND column_name = 'warehouse_uuid'
);
SET @sql := IF(@delivery_warehouse_uuid_missing,
  'ALTER TABLE `biz_delivery_order` ADD COLUMN `warehouse_uuid` VARCHAR(36) DEFAULT NULL COMMENT ''出库仓库'' AFTER `customer_name`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @delivery_warehouse_name_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_delivery_order'
    AND column_name = 'warehouse_name'
);
SET @sql := IF(@delivery_warehouse_name_missing,
  'ALTER TABLE `biz_delivery_order` ADD COLUMN `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT ''出库仓库名称快照'' AFTER `warehouse_uuid`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE biz_finish_roll f
INNER JOIN biz_process_order o
  ON o.uuid = f.order_uuid
 AND o.is_deleted = 0
SET f.stock_in_time = o.back_record_time
WHERE f.stock_in_time IS NULL
  AND f.finish_status = 2
  AND o.back_record_time IS NOT NULL;

UPDATE biz_finish_roll f
INNER JOIN biz_process_order o
  ON o.uuid = f.order_uuid
 AND o.is_deleted = 0
SET f.warehouse_uuid = o.warehouse_uuid
WHERE f.warehouse_uuid IS NULL
  AND o.warehouse_uuid IS NOT NULL;

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
  AND f.is_deleted = 0;

UPDATE biz_delivery_order d
INNER JOIN (
  SELECT dd.delivery_uuid,
         MIN(f.warehouse_uuid) AS warehouse_uuid,
         MAX(w.warehouse_name) AS warehouse_name
  FROM biz_delivery_detail dd
  INNER JOIN biz_finish_roll f
    ON f.uuid = dd.finish_uuid
   AND f.is_deleted = 0
  LEFT JOIN sys_warehouse w
    ON w.uuid = f.warehouse_uuid
   AND w.is_deleted = 0
  WHERE dd.is_deleted = 0
    AND f.warehouse_uuid IS NOT NULL
  GROUP BY dd.delivery_uuid
  HAVING COUNT(DISTINCT f.warehouse_uuid) = 1
) inferred ON inferred.delivery_uuid = d.uuid
SET d.warehouse_uuid = COALESCE(d.warehouse_uuid, inferred.warehouse_uuid),
    d.warehouse_name = COALESCE(d.warehouse_name, inferred.warehouse_name)
WHERE d.warehouse_uuid IS NULL OR d.warehouse_name IS NULL;

SET @inventory_index_missing := (
  SELECT COUNT(*) = 0
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_finish_roll'
    AND index_name = 'idx_finish_inventory_filter'
);
SET @sql := IF(@inventory_index_missing,
  'ALTER TABLE `biz_finish_roll` ADD INDEX `idx_finish_inventory_filter` (`finish_status`, `is_deleted`, `warehouse_uuid`, `stock_in_time`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
