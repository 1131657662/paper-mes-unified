package com.paper.mes.customerdisplay;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerDisplaySchemaContractTest {

    private static final List<String> FINISH_TERMS = List.of(
            "customer_display_weight",
            "request_hash",
            "physical_weight_snapshot",
            "biz_finish_customer_revision",
            "biz_finish_customer_revision_item",
            "uk_finish_customer_revision_no",
            "uk_finish_customer_revision_request",
            "fk_finish_customer_revision_item_finish");
    private static final List<String> DELIVERY_TERMS = List.of(
            "biz_delivery_customer_revision",
            "biz_delivery_customer_revision_item",
            "request_hash",
            "uk_delivery_customer_revision_no",
            "uk_delivery_customer_revision_request",
            "fk_delivery_customer_item_detail");

    @Test
    void migrations_defineNormalizedVersionTablesAndIndexes() throws IOException {
        assertContains(read("sql/V3.38__add_finish_customer_revisions.sql"), FINISH_TERMS);
        assertContains(read("sql/V3.39__add_delivery_customer_revisions.sql"), DELIVERY_TERMS);
    }

    @Test
    void baseline_containsCustomerDisplaySchema() throws IOException {
        String baseline = read("sql/01_schema_v4.1.sql");
        assertContains(baseline, FINISH_TERMS);
        assertContains(baseline, DELIVERY_TERMS);
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private void assertContains(String sql, List<String> terms) {
        for (String term : terms) {
            assertTrue(sql.contains(term), () -> "Missing customer display schema term: " + term);
        }
    }
}
