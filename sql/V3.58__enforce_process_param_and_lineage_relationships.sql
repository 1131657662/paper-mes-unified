-- V3.58: constrain process parameters and active finish/original lineage.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_process_lineage_relationships', 10) INTO @relationship_lock;
SET @relationship_lock_sql := IF(
  @relationship_lock = 1,
  'DO 0',
  'SELECT V3_58_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE relationship_lock_guard FROM @relationship_lock_sql;
EXECUTE relationship_lock_guard;
DEALLOCATE PREPARE relationship_lock_guard;

SET @lineage_relationship_conflicts :=
  (SELECT COUNT(*)
   FROM biz_process_param param
   LEFT JOIN biz_original_roll original
     ON original.uuid = param.original_uuid AND original.order_uuid = param.order_uuid
   WHERE original.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_param param
     LEFT JOIN biz_process_step step
       ON step.uuid = param.step_uuid
      AND step.order_uuid = param.order_uuid
      AND step.original_uuid = param.original_uuid
     WHERE param.step_uuid IS NOT NULL AND step.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_finish_original_rel rel
     LEFT JOIN biz_finish_roll finish
       ON finish.uuid = rel.finish_uuid AND finish.order_uuid = rel.order_uuid
     WHERE finish.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_finish_original_rel rel
     LEFT JOIN biz_original_roll original
       ON original.uuid = rel.original_uuid AND original.order_uuid = rel.order_uuid
     WHERE original.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM (
       SELECT finish_uuid, original_uuid
       FROM biz_finish_original_rel
       WHERE is_deleted = 0
       GROUP BY finish_uuid, original_uuid
       HAVING COUNT(*) > 1
     ) duplicate_lineage);
SET @lineage_relationship_guard_sql := IF(
  @lineage_relationship_conflicts = 0,
  'DO 0',
  'SELECT V3_58_LINEAGE_RELATIONSHIP_CONFLICTS'
);
PREPARE lineage_relationship_guard FROM @lineage_relationship_guard_sql;
EXECUTE lineage_relationship_guard;
DEALLOCATE PREPARE lineage_relationship_guard;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_finish_original_rel'
     AND column_name = 'active_finish_uuid') = 0,
  'ALTER TABLE `biz_finish_original_rel` ADD COLUMN `active_finish_uuid` VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `finish_uuid` ELSE NULL END) STORED AFTER `is_deleted`',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_finish_original_rel'
     AND column_name = 'active_original_uuid') = 0,
  'ALTER TABLE `biz_finish_original_rel` ADD COLUMN `active_original_uuid` VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `original_uuid` ELSE NULL END) STORED AFTER `active_finish_uuid`',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_finish_original_rel'
     AND index_name = 'uk_finish_original_rel_active') = 0,
  'ALTER TABLE `biz_finish_original_rel` ADD UNIQUE KEY `uk_finish_original_rel_active` (`active_finish_uuid`, `active_original_uuid`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_param'
     AND constraint_name = 'fk_process_param_original_scope') = 0,
  'ALTER TABLE `biz_process_param` ADD CONSTRAINT `fk_process_param_original_scope` FOREIGN KEY (`original_uuid`, `order_uuid`) REFERENCES `biz_original_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_param'
     AND constraint_name = 'fk_process_param_step_scope') = 0,
  'ALTER TABLE `biz_process_param` ADD CONSTRAINT `fk_process_param_step_scope` FOREIGN KEY (`step_uuid`, `order_uuid`, `original_uuid`) REFERENCES `biz_process_step` (`uuid`, `order_uuid`, `original_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_finish_original_rel'
     AND constraint_name = 'fk_finish_original_rel_finish_scope') = 0,
  'ALTER TABLE `biz_finish_original_rel` ADD CONSTRAINT `fk_finish_original_rel_finish_scope` FOREIGN KEY (`finish_uuid`, `order_uuid`) REFERENCES `biz_finish_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_finish_original_rel'
     AND constraint_name = 'fk_finish_original_rel_original_scope') = 0,
  'ALTER TABLE `biz_finish_original_rel` ADD CONSTRAINT `fk_finish_original_rel_original_scope` FOREIGN KEY (`original_uuid`, `order_uuid`) REFERENCES `biz_original_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SELECT RELEASE_LOCK('paper_mes_process_lineage_relationships') INTO @relationship_unlock;
