package com.paper.mes.processorder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(37)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ProcessCatalogSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createCatalogTable();
        createUnitTable();
        createBillingModeTable();
        seedCatalog();
        seedUnits();
        seedBillingModes();
        replaceLegacyTypeConstraint();
    }

    private void createCatalogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_process_catalog` (
                  `uuid` varchar(36) NOT NULL, `step_type` tinyint NOT NULL,
                  `process_code` varchar(50) NOT NULL, `process_name` varchar(80) NOT NULL,
                  `process_category` varchar(20) NOT NULL, `pricing_strategy` varchar(30) NOT NULL,
                  `produces_inventory_output` tinyint NOT NULL DEFAULT 0,
                  `allows_loss_recording` tinyint NOT NULL DEFAULT 0,
                  `allows_main_process` tinyint NOT NULL DEFAULT 0, `status` tinyint NOT NULL DEFAULT 1,
                  `sort_no` int NOT NULL DEFAULT 100, `built_in` tinyint NOT NULL DEFAULT 0,
                  `remark` varchar(255) DEFAULT NULL, `is_deleted` tinyint NOT NULL DEFAULT 0,
                  `create_by` varchar(50) DEFAULT NULL, `update_by` varchar(50) DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  `version` int NOT NULL DEFAULT 1, `ext_str1` varchar(255) DEFAULT NULL,
                  `ext_str2` varchar(255) DEFAULT NULL, `ext_num1` decimal(12,3) DEFAULT NULL,
                  `ext_num2` decimal(12,3) DEFAULT NULL, PRIMARY KEY (`uuid`),
                  UNIQUE KEY `uk_process_catalog_step_type` (`step_type`),
                  UNIQUE KEY `uk_process_catalog_code` (`process_code`),
                  KEY `idx_process_catalog_active` (`status`,`is_deleted`,`sort_no`),
                  CONSTRAINT `chk_process_catalog_category` CHECK (`process_category` IN
                    ('PRODUCTION','SERVICE','QUALITY','PACKAGING','LOGISTICS')),
                  CONSTRAINT `chk_process_catalog_strategy` CHECK (`pricing_strategy` IN
                    ('SAW_KNIFE','REWIND_WEIGHT','SERVICE_QUANTITY')),
                  CONSTRAINT `chk_process_catalog_flags` CHECK (
                    `produces_inventory_output` IN (0,1) AND `allows_loss_recording` IN (0,1)
                    AND `allows_main_process` IN (0,1) AND `status` IN (0,1) AND `built_in` IN (0,1))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Process capability catalog'
                """);
    }

    private void createUnitTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_process_catalog_unit` (
                  `catalog_uuid` varchar(36) NOT NULL, `unit_code` varchar(16) NOT NULL,
                  `unit_name` varchar(30) NOT NULL, `is_default` tinyint NOT NULL DEFAULT 0,
                  `default_catalog_uuid` varchar(36) GENERATED ALWAYS AS
                    (CASE WHEN `is_default`=1 THEN `catalog_uuid` ELSE NULL END) STORED,
                  `sort_no` int NOT NULL DEFAULT 100, PRIMARY KEY (`catalog_uuid`,`unit_code`),
                  UNIQUE KEY `uk_process_catalog_default_unit` (`default_catalog_uuid`),
                  CONSTRAINT `fk_process_catalog_unit_catalog` FOREIGN KEY (`catalog_uuid`)
                    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT `chk_process_catalog_unit_default` CHECK (`is_default` IN (0,1))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed process measurement units'
                """);
    }

    private void createBillingModeTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_process_catalog_billing_mode` (
                  `catalog_uuid` varchar(36) NOT NULL, `billing_mode` tinyint NOT NULL,
                  `sort_no` int NOT NULL DEFAULT 100, PRIMARY KEY (`catalog_uuid`,`billing_mode`),
                  CONSTRAINT `fk_process_catalog_billing_catalog` FOREIGN KEY (`catalog_uuid`)
                    REFERENCES `sys_process_catalog` (`uuid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT `chk_process_catalog_billing_mode` CHECK (`billing_mode` IN (1,2,3,4))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Allowed process billing modes'
                """);
    }

    private void seedCatalog() {
        List<ProcessSeed> seeds = List.of(
                new ProcessSeed("process-catalog-saw", 1, "SAW", "锯纸",
                        "PRODUCTION", "SAW_KNIFE", 1, 1, 1, 10),
                new ProcessSeed("process-catalog-rewind", 2, "REWIND", "复卷",
                        "PRODUCTION", "REWIND_WEIGHT", 1, 1, 1, 20),
                new ProcessSeed("process-catalog-strip", 3, "STRIP_SORT", "剥损整理",
                        "SERVICE", "SERVICE_QUANTITY", 0, 1, 0, 30),
                new ProcessSeed("process-catalog-repack", 4, "REPACK", "重新包装",
                        "PACKAGING", "SERVICE_QUANTITY", 0, 0, 0, 40));
        seeds.forEach(this::seedProcess);
    }

    private void seedProcess(ProcessSeed seed) {
        jdbcTemplate.update("""
                INSERT INTO sys_process_catalog
                (uuid,step_type,process_code,process_name,process_category,pricing_strategy,
                 produces_inventory_output,allows_loss_recording,allows_main_process,status,sort_no,built_in,
                 remark,create_by,update_by) VALUES (?,?,?,?,?,?,?,?,?,1,?,1,?,'system','system')
                ON DUPLICATE KEY UPDATE process_name=VALUES(process_name),
                 process_category=VALUES(process_category), pricing_strategy=VALUES(pricing_strategy),
                 produces_inventory_output=VALUES(produces_inventory_output),
                 allows_loss_recording=VALUES(allows_loss_recording),
                 allows_main_process=VALUES(allows_main_process), sort_no=VALUES(sort_no), built_in=1
                """, seed.uuid(), seed.type(), seed.code(), seed.name(), seed.category(), seed.strategy(),
                seed.output(), seed.loss(), seed.main(), seed.sort(), "Legacy type " + seed.type());
    }

    private void seedUnits() {
        jdbcTemplate.batchUpdate("""
                INSERT IGNORE INTO sys_process_catalog_unit
                (catalog_uuid,unit_code,unit_name,is_default,sort_no) VALUES (?,?,?,?,?)
                """, List.of(
                new Object[]{"process-catalog-saw", "KNIFE", "刀", 1, 10},
                new Object[]{"process-catalog-rewind", "TON", "吨", 1, 10},
                new Object[]{"process-catalog-strip", "PIECE", "件", 1, 10},
                new Object[]{"process-catalog-strip", "TON", "吨", 0, 20},
                new Object[]{"process-catalog-repack", "PIECE", "件", 1, 10},
                new Object[]{"process-catalog-repack", "TON", "吨", 0, 20}));
    }

    private void seedBillingModes() {
        jdbcTemplate.batchUpdate("""
                INSERT IGNORE INTO sys_process_catalog_billing_mode
                (catalog_uuid,billing_mode,sort_no) VALUES (?,?,?)
                """, List.of(
                row("saw", 1), row("saw", 2), row("saw", 3), row("saw", 4),
                row("rewind", 1), row("rewind", 2), row("rewind", 3), row("rewind", 4),
                row("strip", 1), row("strip", 3), row("strip", 4),
                row("repack", 1), row("repack", 3), row("repack", 4)));
    }

    private Object[] row(String code, int mode) {
        return new Object[]{"process-catalog-" + code, mode, mode * 10};
    }

    private void replaceLegacyTypeConstraint() {
        if (hasConstraint("chk_process_step_service_basis")) {
            jdbcTemplate.execute("ALTER TABLE biz_process_step DROP CHECK chk_process_step_service_basis");
        }
        if (hasConstraint("chk_process_step_type")) {
            jdbcTemplate.execute("ALTER TABLE biz_process_step DROP CHECK chk_process_step_type");
        }
        if (!hasConstraint("fk_process_step_catalog_type")) {
            jdbcTemplate.execute("""
                    ALTER TABLE biz_process_step ADD CONSTRAINT fk_process_step_catalog_type
                    FOREIGN KEY (step_type) REFERENCES sys_process_catalog(step_type)
                    ON DELETE RESTRICT ON UPDATE RESTRICT
                    """);
        }
    }

    private boolean hasConstraint(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema=DATABASE() AND table_name='biz_process_step'
                  AND constraint_name=?
                """, Integer.class, name);
        return count != null && count > 0;
    }

    private record ProcessSeed(String uuid, int type, String code, String name, String category,
                               String strategy, int output, int loss, int main, int sort) { }
}
