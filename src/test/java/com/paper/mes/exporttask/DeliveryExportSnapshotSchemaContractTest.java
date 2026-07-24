package com.paper.mes.exporttask;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryExportSnapshotSchemaContractTest {

    @Test
    void migrationCreatesOwnedSnapshotHeaderAndRows() throws IOException {
        assertSnapshotSchema(read("sql/V3.45__add_delivery_export_snapshots.sql"));
    }

    @Test
    void baselineContainsOwnedSnapshotHeaderAndRows() throws IOException {
        assertSnapshotSchema(read("sql/01_schema_v4.1.sql"));
    }

    private void assertSnapshotSchema(String sql) {
        assertThat(sql)
                .contains("sys_export_snapshot", "sys_export_snapshot_row")
                .contains("PRIMARY KEY (`snapshot_uuid`, `row_no`)")
                .contains("UNIQUE KEY `uk_export_snapshot_task` (`task_uuid`)")
                .contains("REFERENCES `sys_export_task` (`uuid`) ON DELETE CASCADE")
                .contains("REFERENCES `sys_export_snapshot` (`uuid`) ON DELETE CASCADE")
                .contains("CHECK (`row_count` >= 0)", "CHECK (`row_no` > 0)");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
