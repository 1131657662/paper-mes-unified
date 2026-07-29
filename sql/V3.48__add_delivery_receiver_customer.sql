-- V3.48: separate the goods owner from the optional receiving customer on delivery orders.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_delivery_receiver_customer', 10) INTO @delivery_receiver_customer_lock;
SET @delivery_receiver_customer_guard_sql = IF(
  @delivery_receiver_customer_lock = 1,
  'DO 0',
  'SELECT V3_48_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE delivery_receiver_customer_guard FROM @delivery_receiver_customer_guard_sql;
EXECUTE delivery_receiver_customer_guard;
DEALLOCATE PREPARE delivery_receiver_customer_guard;

SET @receiver_customer_name_missing = (
  SELECT COUNT(*) = 0
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_delivery_order'
    AND column_name = 'receiver_customer_name'
);
SET @sql = IF(@receiver_customer_name_missing,
  'ALTER TABLE `biz_delivery_order` ADD COLUMN `receiver_customer_name` VARCHAR(100) DEFAULT NULL COMMENT ''收货客户名称（货主告知后手工填写）'' AFTER `customer_name`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT RELEASE_LOCK('paper_mes_delivery_receiver_customer') INTO @delivery_receiver_customer_unlock;
