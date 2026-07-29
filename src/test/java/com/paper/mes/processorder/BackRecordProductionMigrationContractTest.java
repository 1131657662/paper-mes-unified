package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BackRecordProductionMigrationContractTest {

    @Test
    void migration_requiresNamedLockAndAddsProductionResultSchemaIdempotently() throws IOException {
        String migration = read("sql/V3.47__add_back_record_production_results.sql");

        assertThat(migration).contains(
                "SELECT GET_LOCK('paper_mes_back_record_production_result', 10)",
                "@back_record_production_lock = 1",
                "SELECT V3_47_MIGRATION_LOCK_NOT_ACQUIRED",
                "production_result TINYINT DEFAULT NULL",
                "1计划产出 2正常产出 3计划未产出 4实际新增产出",
                "production_adjustment_reason VARCHAR(255) DEFAULT NULL",
                "回录产出调整原因",
                "CREATE INDEX idx_finish_production_result",
                "SELECT RELEASE_LOCK('paper_mes_back_record_production_result')");
        assertThat(migration).containsOnlyOnce("START TRANSACTION");
    }

    @Test
    void baseline_containsBackRecordProductionResultSchema() throws IOException {
        String baseline = read("sql/01_schema_v4.1.sql");

        assertThat(baseline).contains(
                "`production_result`    TINYINT",
                "`production_adjustment_reason` VARCHAR(255)",
                "`idx_finish_production_result` (`order_uuid`, `production_result`, `finish_status`)");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
