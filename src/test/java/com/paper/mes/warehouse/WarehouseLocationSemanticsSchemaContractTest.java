package com.paper.mes.warehouse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseLocationSemanticsSchemaContractTest {

    @Test
    void migrationRelabelsLocationWithoutChangingWarehouseData() throws IOException {
        String migration = read("sql/V3.60__clarify_warehouse_location_semantics.sql");

        assertThat(migration).contains(
                "SET SESSION lock_wait_timeout = 5",
                "SET SESSION innodb_lock_wait_timeout = 5",
                "paper_mes_warehouse_location_semantics",
                "V3_60_MIGRATION_LOCK_NOT_ACQUIRED",
                "information_schema.columns",
                "MODIFY COLUMN `location` VARCHAR(255) DEFAULT NULL COMMENT ''仓库地址/说明''");
        assertThat(migration).doesNotContain(
                "UPDATE `sys_warehouse`",
                "DELETE FROM `sys_warehouse`",
                "ADD COLUMN `location`",
                "DROP COLUMN `location`",
                "RENAME COLUMN `location`");
    }

    @Test
    void canonicalSchemaUsesAddressDescriptionSemantics() throws IOException {
        String schema = read("sql/01_schema_v4.1.sql");

        assertThat(schema).contains(
                "`location`        VARCHAR(255) DEFAULT NULL            COMMENT '仓库地址/说明'");
        assertThat(read("sql/schema-baseline.version").trim()).isEqualTo("3.60");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
