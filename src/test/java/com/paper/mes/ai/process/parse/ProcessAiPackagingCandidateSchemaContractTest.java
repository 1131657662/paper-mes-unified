package com.paper.mes.ai.process.parse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiPackagingCandidateSchemaContractTest {

    @Test
    void migrationDefinesDurablePendingSavedAndDismissedStates() throws Exception {
        String sql = Files.readString(Path.of(
                "sql/V3.72__add_ai_packaging_candidate_state.sql"));

        assertThat(sql).contains("biz_process_ai_packaging_candidate");
        assertThat(sql).contains("'PENDING', 'SAVED', 'DISMISSED'");
        assertThat(sql).contains("uk_ai_packaging_candidate_parse_owner");
        assertThat(sql).contains("idx_ai_packaging_candidate_pending");
        assertThat(sql).contains("fk_ai_packaging_candidate_parse");
    }
}
