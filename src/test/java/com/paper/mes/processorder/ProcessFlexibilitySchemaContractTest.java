package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessFlexibilitySchemaContractTest {

    private static final String[] COLUMNS = {
            "customer_finish_width",
            "customer_spec_override_reason",
            "customer_spec_override_by",
            "customer_spec_override_at",
            "width_difference_policy",
            "planned_loss_width",
            "planned_loss_weight"
    };

    @Test
    void migration_isIdempotentAndContainsEveryFlexibilityColumn() throws IOException {
        String sql = read("sql/V3.35__add_customer_spec_and_width_policy.sql");

        assertTrue(sql.contains("information_schema.columns"));
        assertColumns(sql);
    }

    @Test
    void baseline_containsEveryFlexibilityColumn() throws IOException {
        assertColumns(read("sql/01_schema_v4.1.sql"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private void assertColumns(String sql) {
        for (String column : COLUMNS) {
            assertTrue(sql.contains(column), () -> "Missing process flexibility column: " + column);
        }
    }
}
