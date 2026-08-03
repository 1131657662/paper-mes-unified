-- V3.55: anchor process rows to one order scope before adding deeper route constraints.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_base_relationships', 10) INTO @relationship_lock;
SET @relationship_lock_sql := IF(
  @relationship_lock = 1,
  'DO 0',
  'SELECT V3_55_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE relationship_lock_guard FROM @relationship_lock_sql;
EXECUTE relationship_lock_guard;
DEALLOCATE PREPARE relationship_lock_guard;

SET @base_relationship_conflicts :=
  (SELECT COUNT(*)
   FROM biz_original_roll original
   LEFT JOIN biz_process_order process_order ON process_order.uuid = original.order_uuid
   WHERE process_order.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_step step
     LEFT JOIN biz_original_roll original
       ON original.uuid = step.original_uuid
      AND original.order_uuid = step.order_uuid
     WHERE original.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_finish_roll finish
     LEFT JOIN biz_process_order process_order ON process_order.uuid = finish.order_uuid
     WHERE process_order.uuid IS NULL);
SET @base_relationship_guard_sql := IF(
  @base_relationship_conflicts = 0,
  'DO 0',
  'SELECT V3_55_BASE_RELATIONSHIP_CONFLICTS'
);
PREPARE base_relationship_guard FROM @base_relationship_guard_sql;
EXECUTE base_relationship_guard;
DEALLOCATE PREPARE base_relationship_guard;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_original_roll'
     AND index_name = 'uk_original_roll_scope') = 0,
  'ALTER TABLE `biz_original_roll` ADD UNIQUE KEY `uk_original_roll_scope` (`uuid`, `order_uuid`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
     AND index_name = 'uk_process_step_scope') = 0,
  'ALTER TABLE `biz_process_step` ADD UNIQUE KEY `uk_process_step_scope` (`uuid`, `order_uuid`, `original_uuid`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_step'
     AND index_name = 'uk_process_step_stage_scope') = 0,
  'ALTER TABLE `biz_process_step` ADD UNIQUE KEY `uk_process_step_stage_scope` (`uuid`, `order_uuid`, `original_uuid`, `stage_level`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_finish_roll'
     AND index_name = 'uk_finish_roll_scope') = 0,
  'ALTER TABLE `biz_finish_roll` ADD UNIQUE KEY `uk_finish_roll_scope` (`uuid`, `order_uuid`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_original_roll'
     AND constraint_name = 'fk_original_roll_order') = 0,
  'ALTER TABLE `biz_original_roll` ADD CONSTRAINT `fk_original_roll_order` FOREIGN KEY (`order_uuid`) REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_step'
     AND constraint_name = 'fk_process_step_original_scope') = 0,
  'ALTER TABLE `biz_process_step` ADD CONSTRAINT `fk_process_step_original_scope` FOREIGN KEY (`original_uuid`, `order_uuid`) REFERENCES `biz_original_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_finish_roll'
     AND constraint_name = 'fk_finish_roll_order') = 0,
  'ALTER TABLE `biz_finish_roll` ADD CONSTRAINT `fk_finish_roll_order` FOREIGN KEY (`order_uuid`) REFERENCES `biz_process_order` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SELECT RELEASE_LOCK('paper_mes_process_base_relationships') INTO @relationship_unlock;
