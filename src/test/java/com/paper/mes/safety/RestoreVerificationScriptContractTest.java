package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RestoreVerificationScriptContractTest {

    @Test
    void linuxRestoreVerification_whenFailureOccurs_dropsTemporaryDatabase() throws Exception {
        String script = Files.readString(Path.of("deploy/verify-backup-restore.example.sh"));

        assertContainsAll(script,
                "local exit_code=$?",
                "restore_created=false",
                "[ \"${exit_code}\" -ne 0 ] || [ \"${DROP_AFTER_VERIFY}\" = \"true\" ]",
                "DROP DATABASE IF EXISTS",
                "restore_created=true",
                "trap cleanup EXIT");
        assertTrue(script.indexOf("restore_created=true")
                > script.indexOf("CREATE DATABASE"));
    }

    @Test
    void linuxRestoreVerification_requiresCoreBusinessTable() throws Exception {
        String script = Files.readString(Path.of("deploy/verify-backup-restore.example.sh"));

        assertContainsAll(script,
                "table_name='biz_process_order'",
                "required table biz_process_order is missing",
                "SELECT COUNT(*) FROM biz_process_order;");
        assertFalse(script.contains("|| echo \"n/a\""),
                "核心表查询失败不能被降级为成功结果");
    }

    @Test
    void productionRestore_preflightsArchiveBeforeDroppingTargetDatabase() throws Exception {
        String script = Files.readString(Path.of("deploy/restore-paper-mes.example.sh"));

        assertContainsAll(script,
                "PREFLIGHT_DB_NAME=",
                "create_database \"${PREFLIGHT_DB_NAME}\"",
                "import_database \"${PREFLIGHT_DB_NAME}\"",
                "assert_required_schema \"${PREFLIGHT_DB_NAME}\"",
                "create_database \"${TARGET_DB_NAME}\"",
                "assert_required_schema \"${TARGET_DB_NAME}\"",
                "preflight_created=true",
                "trap cleanup EXIT");
        assertTrue(script.indexOf("assert_required_schema \"${PREFLIGHT_DB_NAME}\"")
                < script.indexOf("create_database \"${TARGET_DB_NAME}\""));
    }

    private void assertContainsAll(String source, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(source.contains(fragment), "缺少恢复清理保护: " + fragment);
        }
    }
}
