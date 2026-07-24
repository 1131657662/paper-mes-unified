package com.paper.mes.machine.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(41)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MachineCapabilitySchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addResourceKind();
        addResourceKindConstraint();
        createCapabilityTable();
        migrateLegacyCapabilities();
        assignSingleCandidateDefaults();
    }

    private void addResourceKindConstraint() {
        if (hasConstraint("sys_machine", "chk_machine_resource_kind")) return;
        jdbcTemplate.execute("""
                ALTER TABLE sys_machine ADD CONSTRAINT chk_machine_resource_kind
                CHECK (resource_kind IN ('MACHINE','WORKSTATION'))
                """);
    }

    private void addResourceKind() {
        if (!hasColumn("sys_machine", "resource_kind")) {
            jdbcTemplate.execute("""
                    ALTER TABLE sys_machine ADD COLUMN resource_kind varchar(20) NULL
                    COMMENT 'MACHINE设备 WORKSTATION工位' AFTER machine_type
                    """);
        }
        jdbcTemplate.update("""
                UPDATE sys_machine SET resource_kind='MACHINE'
                WHERE resource_kind IS NULL OR TRIM(resource_kind)=''
                """);
        if (isNullable("sys_machine", "resource_kind")) {
            jdbcTemplate.execute("""
                    ALTER TABLE sys_machine MODIFY COLUMN resource_kind varchar(20) NOT NULL
                    DEFAULT 'MACHINE' COMMENT 'MACHINE设备 WORKSTATION工位'
                    """);
        }
    }

    private void createCapabilityTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_machine_process_capability (
                  uuid varchar(36) NOT NULL, machine_uuid varchar(36) NOT NULL,
                  catalog_uuid varchar(36) NOT NULL, is_default tinyint NOT NULL DEFAULT 0,
                  priority int NOT NULL DEFAULT 100, min_width int DEFAULT NULL,
                  max_width int DEFAULT NULL, max_roll_weight decimal(12,3) DEFAULT NULL,
                  max_diameter int DEFAULT NULL, remark varchar(255) DEFAULT NULL,
                  is_deleted tinyint NOT NULL DEFAULT 0, create_by varchar(50) DEFAULT NULL,
                  update_by varchar(50) DEFAULT NULL,
                  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  version int NOT NULL DEFAULT 1, ext_str1 varchar(255) DEFAULT NULL,
                  ext_str2 varchar(255) DEFAULT NULL, ext_num1 decimal(12,3) DEFAULT NULL,
                  ext_num2 decimal(12,3) DEFAULT NULL,
                  active_machine_catalog_key varchar(80) GENERATED ALWAYS AS
                    (CASE WHEN is_deleted=0 THEN CONCAT(machine_uuid,':',catalog_uuid) ELSE NULL END) STORED,
                  active_default_catalog_key varchar(36) GENERATED ALWAYS AS
                    (CASE WHEN is_deleted=0 AND is_default=1 THEN catalog_uuid ELSE NULL END) STORED,
                  PRIMARY KEY (uuid),
                  UNIQUE KEY uk_machine_capability_active (active_machine_catalog_key),
                  UNIQUE KEY uk_machine_capability_default (active_default_catalog_key),
                  KEY idx_machine_capability_machine (machine_uuid,is_deleted),
                  KEY idx_machine_capability_catalog (catalog_uuid,is_deleted,priority),
                  CONSTRAINT fk_machine_capability_machine FOREIGN KEY (machine_uuid)
                    REFERENCES sys_machine(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_machine_capability_catalog FOREIGN KEY (catalog_uuid)
                    REFERENCES sys_process_catalog(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT chk_machine_capability_default CHECK (is_default IN (0,1)),
                  CONSTRAINT chk_machine_capability_priority CHECK (priority BETWEEN 1 AND 9999),
                  CONSTRAINT chk_machine_capability_width CHECK (
                    (min_width IS NULL OR min_width>0) AND (max_width IS NULL OR max_width>0)
                    AND (min_width IS NULL OR max_width IS NULL OR min_width<=max_width)),
                  CONSTRAINT chk_machine_capability_limits CHECK (
                    (max_roll_weight IS NULL OR max_roll_weight>0)
                    AND (max_diameter IS NULL OR max_diameter>0))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机台/工位支持的工艺能力'
                """);
    }

    private void migrateLegacyCapabilities() {
        jdbcTemplate.update("""
                INSERT IGNORE INTO sys_machine_process_capability
                (uuid,machine_uuid,catalog_uuid,priority,remark,create_by,update_by)
                SELECT UUID(),m.uuid,c.uuid,100,'由历史机台类型迁移','system','system'
                FROM sys_machine m JOIN sys_process_catalog c
                  ON (m.machine_type=1 AND c.process_code='SAW')
                  OR (m.machine_type=2 AND c.process_code='REWIND')
                  OR (m.machine_type=3 AND c.process_code IN ('SAW','REWIND'))
                WHERE m.is_deleted=0 AND c.is_deleted=0
                """);
    }

    private void assignSingleCandidateDefaults() {
        jdbcTemplate.update("""
                UPDATE sys_machine_process_capability capability
                JOIN (SELECT MIN(mc.uuid) capability_uuid
                  FROM sys_machine_process_capability mc
                  JOIN sys_machine m ON m.uuid=mc.machine_uuid
                  WHERE mc.is_deleted=0 AND m.is_deleted=0 AND m.status=1
                  GROUP BY mc.catalog_uuid HAVING COUNT(*)=1) single_capability
                  ON single_capability.capability_uuid=capability.uuid
                SET capability.is_default=1 WHERE capability.is_default=0
                """);
    }

    private boolean hasColumn(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name=? AND column_name=?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean hasConstraint(String table, String constraint) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema=DATABASE() AND table_name=? AND constraint_name=?
                """, Integer.class, table, constraint);
        return count != null && count > 0;
    }

    private boolean isNullable(String table, String column) {
        String nullable = jdbcTemplate.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name=? AND column_name=?
                """, String.class, table, column);
        return "YES".equals(nullable);
    }
}
