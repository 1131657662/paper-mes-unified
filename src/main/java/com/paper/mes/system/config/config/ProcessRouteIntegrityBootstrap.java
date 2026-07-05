package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(32)
public class ProcessRouteIntegrityBootstrap implements ApplicationRunner {

    private static final String PROCESS_STEP_TABLE = "biz_process_step";
    private static final String STEP_MACHINE_UUID_COLUMN = "machine_uuid";
    private static final String STEP_MACHINE_NAME_COLUMN = "machine_name_snap";
    private static final String STEP_MACHINE_INDEX = "idx_process_step_machine_uuid";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addProcessStepMachineColumns();
        backfillProcessStepMachines();
        addProcessStepMachineIndex();
        createStageInputRelTable();
    }

    private void addProcessStepMachineColumns() {
        if (!columnExists(PROCESS_STEP_TABLE, STEP_MACHINE_UUID_COLUMN)) {
            jdbcTemplate.execute("""
                    ALTER TABLE `biz_process_step`
                    ADD COLUMN `machine_uuid` VARCHAR(36) DEFAULT NULL
                    COMMENT '工序加工机台' AFTER `step_name`
                    """);
        }
        if (!columnExists(PROCESS_STEP_TABLE, STEP_MACHINE_NAME_COLUMN)) {
            jdbcTemplate.execute("""
                    ALTER TABLE `biz_process_step`
                    ADD COLUMN `machine_name_snap` VARCHAR(100) DEFAULT NULL
                    COMMENT '工序机台名称快照' AFTER `machine_uuid`
                    """);
        }
    }

    private void backfillProcessStepMachines() {
        jdbcTemplate.update("""
                UPDATE biz_process_step ps
                JOIN biz_original_roll r
                  ON r.uuid = ps.original_uuid
                LEFT JOIN sys_machine m
                  ON m.uuid = r.machine_uuid
                SET ps.machine_uuid = COALESCE(ps.machine_uuid, r.machine_uuid),
                    ps.machine_name_snap = COALESCE(ps.machine_name_snap, m.machine_name)
                WHERE ps.is_deleted = 0
                  AND ps.machine_uuid IS NULL
                  AND r.machine_uuid IS NOT NULL
                """);
    }

    private void addProcessStepMachineIndex() {
        if (indexExists(PROCESS_STEP_TABLE, STEP_MACHINE_INDEX)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_process_step`
                ADD INDEX `idx_process_step_machine_uuid` (`machine_uuid`)
                """);
    }

    private void createStageInputRelTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_process_stage_input_rel` (
                  `uuid`              VARCHAR(36)   NOT NULL                COMMENT '阶段输入关联主键',
                  `order_uuid`        VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
                  `original_uuid`     VARCHAR(36)   NOT NULL                COMMENT '根母卷UUID',
                  `step_uuid`         VARCHAR(36)   NOT NULL                COMMENT '消费这些阶段产出的工序UUID',
                  `input_output_uuid` VARCHAR(36)   NOT NULL                COMMENT '被消费的阶段产出UUID',
                  `source_step_uuid`  VARCHAR(36)   DEFAULT NULL            COMMENT '阶段产出来源工序UUID',
                  `input_sort`        INT           NOT NULL DEFAULT 1      COMMENT '本工序输入顺序',
                  `stage_level`       INT           NOT NULL DEFAULT 1      COMMENT '消费工序所在阶段层级',
                  `is_deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
                  `create_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
                  `update_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
                  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  `version`           INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
                  `ext_str1`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
                  `ext_str2`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
                  `ext_num1`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
                  `ext_num2`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
                  PRIMARY KEY (`uuid`),
                  KEY `idx_order_uuid` (`order_uuid`),
                  KEY `idx_original_uuid` (`original_uuid`),
                  KEY `idx_step_uuid` (`step_uuid`),
                  KEY `idx_input_output_uuid` (`input_output_uuid`),
                  KEY `idx_source_step_uuid` (`source_step_uuid`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺阶段输入关联表'
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
