package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerRevisionAuditSchemaContractTest {

    @Test
    void migrationAddsOperandToBothRevisionItemTablesIdempotently() throws IOException {
        String sql = read("sql/V3.40__add_customer_revision_weight_operand.sql");

        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("biz_finish_customer_revision_item"));
        assertTrue(sql.contains("biz_delivery_customer_revision_item"));
        assertTrue(sql.contains("weight_operand"));
    }

    @Test
    void baselinePreservesWeightOperandForCustomerRevisionAudit() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");

        assertTrue(occurrences(sql, "`weight_operand` DECIMAL(20,6)") >= 2);
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private long occurrences(String text, String token) {
        return text.lines().filter(line -> line.contains(token)).count();
    }
}
