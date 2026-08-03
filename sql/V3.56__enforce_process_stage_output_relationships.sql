-- V3.56: keep every stage output inside its order/original route scope.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_stage_output_relationships', 10) INTO @relationship_lock;
SET @relationship_lock_sql := IF(
  @relationship_lock = 1,
  'DO 0',
  'SELECT V3_56_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE relationship_lock_guard FROM @relationship_lock_sql;
EXECUTE relationship_lock_guard;
DEALLOCATE PREPARE relationship_lock_guard;

SET @stage_output_conflicts :=
  (SELECT COUNT(*)
   FROM biz_process_stage_output output
   LEFT JOIN biz_original_roll original
     ON original.uuid = output.original_uuid AND original.order_uuid = output.order_uuid
   WHERE original.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_stage_output output
     LEFT JOIN biz_process_step step
       ON step.uuid = output.step_uuid
      AND step.order_uuid = output.order_uuid
      AND step.original_uuid = output.original_uuid
      AND step.stage_level = output.stage_level
     WHERE step.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_stage_output output
     LEFT JOIN biz_process_stage_output parent
       ON parent.uuid = output.parent_output_uuid
      AND parent.order_uuid = output.order_uuid
      AND parent.original_uuid = output.original_uuid
     WHERE output.parent_output_uuid IS NOT NULL AND parent.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_stage_output output
     LEFT JOIN biz_finish_roll finish
       ON finish.uuid = output.finish_roll_uuid AND finish.order_uuid = output.order_uuid
     WHERE output.finish_roll_uuid IS NOT NULL AND finish.uuid IS NULL);
SET @stage_output_guard_sql := IF(
  @stage_output_conflicts = 0,
  'DO 0',
  'SELECT V3_56_STAGE_OUTPUT_RELATIONSHIP_CONFLICTS'
);
PREPARE stage_output_guard FROM @stage_output_guard_sql;
EXECUTE stage_output_guard;
DEALLOCATE PREPARE stage_output_guard;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_stage_output'
     AND index_name = 'uk_stage_output_scope') = 0,
  'ALTER TABLE `biz_process_stage_output` ADD UNIQUE KEY `uk_stage_output_scope` (`uuid`, `order_uuid`, `original_uuid`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_process_stage_output'
     AND index_name = 'uk_stage_output_source_scope') = 0,
  'ALTER TABLE `biz_process_stage_output` ADD UNIQUE KEY `uk_stage_output_source_scope` (`uuid`, `order_uuid`, `original_uuid`, `step_uuid`)',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_output'
     AND constraint_name = 'fk_stage_output_original_scope') = 0,
  'ALTER TABLE `biz_process_stage_output` ADD CONSTRAINT `fk_stage_output_original_scope` FOREIGN KEY (`original_uuid`, `order_uuid`) REFERENCES `biz_original_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_output'
     AND constraint_name = 'fk_stage_output_step_scope') = 0,
  'ALTER TABLE `biz_process_stage_output` ADD CONSTRAINT `fk_stage_output_step_scope` FOREIGN KEY (`step_uuid`, `order_uuid`, `original_uuid`, `stage_level`) REFERENCES `biz_process_step` (`uuid`, `order_uuid`, `original_uuid`, `stage_level`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_output'
     AND constraint_name = 'fk_stage_output_parent_scope') = 0,
  'ALTER TABLE `biz_process_stage_output` ADD CONSTRAINT `fk_stage_output_parent_scope` FOREIGN KEY (`parent_output_uuid`, `order_uuid`, `original_uuid`) REFERENCES `biz_process_stage_output` (`uuid`, `order_uuid`, `original_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_output'
     AND constraint_name = 'fk_stage_output_finish_scope') = 0,
  'ALTER TABLE `biz_process_stage_output` ADD CONSTRAINT `fk_stage_output_finish_scope` FOREIGN KEY (`finish_roll_uuid`, `order_uuid`) REFERENCES `biz_finish_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SELECT RELEASE_LOCK('paper_mes_stage_output_relationships') INTO @relationship_unlock;
