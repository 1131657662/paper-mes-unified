package com.paper.mes.settle;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCandidateQueryContractTest {

    @Test
    void candidateMapper_excludesLargeSnapshotColumns() throws Exception {
        String mapper = readMapper();

        assertThat(mapper).contains("accounting_date", "total_extra_amount", "total_amount")
                .doesNotContain("snap_print", "snap_finish", "remark_long");
    }

    @Test
    void candidateMapper_keepsStableOrdering() throws Exception {
        assertThat(readMapper()).contains("ORDER BY accounting_date ASC, order_no ASC, uuid ASC");
    }

    @Test
    void candidateMapper_trimsKeywordParameter() throws Exception {
        assertThat(readMapper()).contains("query.keyword.trim()", "#{keyword}")
                .doesNotContain("#{query.keyword}");
    }

    @Test
    void migration_guardsDuplicateIndexCreation() throws Exception {
        assertThat(readMigration()).contains("information_schema.statistics", "lock_wait_timeout",
                "idx_settle_candidate_query");
    }

    @Test
    void migration_usesCandidateFilterAndOrderColumns() throws Exception {
        assertThat(readMigration()).contains("is_deleted`, `order_status`, `accounting_date`, `order_no`, `uuid");
    }

    private String readMapper() throws Exception {
        return Files.readString(Path.of("src/main/resources/mapper/processorder/ProcessOrderMapper.xml"),
                StandardCharsets.UTF_8);
    }

    private String readMigration() throws Exception {
        return Files.readString(Path.of("sql/V3.49__add_settlement_candidate_query_index.sql"),
                StandardCharsets.UTF_8);
    }
}
