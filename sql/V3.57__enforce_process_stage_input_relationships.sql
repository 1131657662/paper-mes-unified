-- V3.57: bind stage inputs to the consuming route and source output scope.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;

SELECT GET_LOCK('paper_mes_stage_input_relationships', 10) INTO @relationship_lock;
SET @relationship_lock_sql := IF(
  @relationship_lock = 1,
  'DO 0',
  'SELECT V3_57_MIGRATION_LOCK_NOT_ACQUIRED'
);
PREPARE relationship_lock_guard FROM @relationship_lock_sql;
EXECUTE relationship_lock_guard;
DEALLOCATE PREPARE relationship_lock_guard;

SET @stage_input_conflicts :=
  (SELECT COUNT(*)
   FROM biz_process_stage_input_rel input_rel
   LEFT JOIN biz_original_roll original
     ON original.uuid = input_rel.original_uuid AND original.order_uuid = input_rel.order_uuid
   WHERE original.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_stage_input_rel input_rel
     LEFT JOIN biz_process_step step
       ON step.uuid = input_rel.step_uuid
      AND step.order_uuid = input_rel.order_uuid
      AND step.original_uuid = input_rel.original_uuid
      AND step.stage_level = input_rel.stage_level
     WHERE step.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_stage_input_rel input_rel
     LEFT JOIN biz_process_stage_output output
       ON output.uuid = input_rel.input_output_uuid
      AND output.order_uuid = input_rel.order_uuid
      AND output.original_uuid = input_rel.original_uuid
     WHERE output.uuid IS NULL)
  + (SELECT COUNT(*)
     FROM biz_process_stage_input_rel input_rel
     LEFT JOIN biz_process_stage_output source_output
       ON source_output.uuid = input_rel.input_output_uuid
      AND source_output.order_uuid = input_rel.order_uuid
      AND source_output.original_uuid = input_rel.original_uuid
      AND source_output.step_uuid = input_rel.source_step_uuid
     WHERE input_rel.source_step_uuid IS NOT NULL AND source_output.uuid IS NULL);
SET @stage_input_guard_sql := IF(
  @stage_input_conflicts = 0,
  'DO 0',
  'SELECT V3_57_STAGE_INPUT_RELATIONSHIP_CONFLICTS'
);
PREPARE stage_input_guard FROM @stage_input_guard_sql;
EXECUTE stage_input_guard;
DEALLOCATE PREPARE stage_input_guard;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_input_rel'
     AND constraint_name = 'fk_stage_input_original_scope') = 0,
  'ALTER TABLE `biz_process_stage_input_rel` ADD CONSTRAINT `fk_stage_input_original_scope` FOREIGN KEY (`original_uuid`, `order_uuid`) REFERENCES `biz_original_roll` (`uuid`, `order_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_input_rel'
     AND constraint_name = 'fk_stage_input_step_scope') = 0,
  'ALTER TABLE `biz_process_stage_input_rel` ADD CONSTRAINT `fk_stage_input_step_scope` FOREIGN KEY (`step_uuid`, `order_uuid`, `original_uuid`, `stage_level`) REFERENCES `biz_process_step` (`uuid`, `order_uuid`, `original_uuid`, `stage_level`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_input_rel'
     AND constraint_name = 'fk_stage_input_output_scope') = 0,
  'ALTER TABLE `biz_process_stage_input_rel` ADD CONSTRAINT `fk_stage_input_output_scope` FOREIGN KEY (`input_output_uuid`, `order_uuid`, `original_uuid`) REFERENCES `biz_process_stage_output` (`uuid`, `order_uuid`, `original_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SET @relationship_ddl_sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE() AND table_name = 'biz_process_stage_input_rel'
     AND constraint_name = 'fk_stage_input_source_output_scope') = 0,
  'ALTER TABLE `biz_process_stage_input_rel` ADD CONSTRAINT `fk_stage_input_source_output_scope` FOREIGN KEY (`input_output_uuid`, `order_uuid`, `original_uuid`, `source_step_uuid`) REFERENCES `biz_process_stage_output` (`uuid`, `order_uuid`, `original_uuid`, `step_uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT',
  'DO 0'
);
PREPARE relationship_ddl FROM @relationship_ddl_sql;
EXECUTE relationship_ddl;
DEALLOCATE PREPARE relationship_ddl;

SELECT RELEASE_LOCK('paper_mes_stage_input_relationships') INTO @relationship_unlock;
