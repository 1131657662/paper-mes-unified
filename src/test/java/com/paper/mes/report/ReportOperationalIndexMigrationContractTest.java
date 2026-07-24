package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportOperationalIndexMigrationContractTest {
    private static final String[] INDEX_NAMES = {
            "idx_report_settle_scope",
            "idx_report_receive_scope",
            "idx_report_inventory_scope",
            "idx_report_delivery_scope",
            "idx_report_delivery_detail"
    };

    @Test
    void migration_isIdempotentAndAddsEveryTopicIndex() throws IOException {
        String sql = Files.readString(Path.of("sql/V3.32__add_operational_report_indexes.sql"),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("information_schema.statistics"));
        assertContainsEveryIndex(sql);
    }

    @Test
    void baselineSchema_containsEveryTopicIndexForFreshInstallations() throws IOException {
        String sql = Files.readString(Path.of("sql/01_schema_v4.1.sql"), StandardCharsets.UTF_8);

        assertContainsEveryIndex(sql);
    }

    private void assertContainsEveryIndex(String sql) {
        for (String indexName : INDEX_NAMES) {
            assertTrue(sql.contains(indexName), () -> "Missing report index: " + indexName);
        }
    }
}
