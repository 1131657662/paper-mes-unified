package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderAppendMigrationContractTest {

    @Test
    void migrationCreatesPersistentTablesAndActiveSessionUniqueness() throws IOException {
        String script = Files.readString(
                Path.of("sql/V3.61__add_process_order_append_sessions.sql"));

        assertThat(script).contains("CREATE TABLE IF NOT EXISTS `biz_process_order_append_session`");
        assertThat(script).contains("CREATE TABLE IF NOT EXISTS `biz_process_order_append_roll`");
        assertThat(script).contains("`active_order_uuid` VARCHAR(36) GENERATED ALWAYS AS");
        assertThat(script).contains("`uk_process_append_active_order` (`active_order_uuid`)");
        assertThat(script).contains("V3_61_DUPLICATE_ACTIVE_APPEND_SESSIONS");
    }
}
