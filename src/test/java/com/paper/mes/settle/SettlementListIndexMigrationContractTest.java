package com.paper.mes.settle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementListIndexMigrationContractTest {
    private static final String INDEX = "idx_settle_list_page";

    @Test
    void migration_isIdempotentAndAddsListPageIndex() throws IOException {
        String sql = read("sql/V3.44__add_settlement_list_page_index.sql");

        assertThat(sql)
                .contains("information_schema.statistics", "lock_wait_timeout", INDEX)
                .contains("is_deleted, create_time, uuid");
    }

    @Test
    void baselineSchema_containsListPageIndex() throws IOException {
        String sql = read("sql/01_schema_v4.1.sql");

        assertThat(sql).contains("`idx_settle_list_page` (`is_deleted`, `create_time`, `uuid`)");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
