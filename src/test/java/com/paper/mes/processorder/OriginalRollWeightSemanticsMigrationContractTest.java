package com.paper.mes.processorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OriginalRollWeightSemanticsMigrationContractTest {

    @Test
    void migrationBackfillsOnlyActiveOrdersAndChecksEveryMergedSource() throws IOException {
        String migration = read("sql/V3.65__add_original_roll_weight_semantics.sql");

        assertThat(migration).contains(
                "JOIN biz_process_order process_order ON process_order.uuid = step.order_uuid",
                "process_order.order_status BETWEEN 0 AND 3",
                "JOIN biz_finish_original_rel source_rel",
                "source_roll.actual_weight IS NULL OR source_roll.actual_weight <= 0");
        assertThat(migration.indexOf("ALTER TABLE biz_finish_original_rel ADD COLUMN consume_ratio"))
                .isLessThan(migration.indexOf("UPDATE biz_process_step step"));
    }

    @Test
    void completedAndSettledLegacyStepsRemainUnclassifiedForCompatibility() throws IOException {
        String migration = read("sql/V3.65__add_original_roll_weight_semantics.sql");

        assertThat(migration).doesNotContain(
                "process_order.order_status BETWEEN 0 AND 4",
                "process_order.order_status BETWEEN 0 AND 5");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
