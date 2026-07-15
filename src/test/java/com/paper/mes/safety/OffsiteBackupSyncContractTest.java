package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OffsiteBackupSyncContractTest {

    @Test
    void syncScript_copiesByChecksumAndWritesAtomicSafeStatus() throws Exception {
        String script = source("deploy/sync-backups-rclone.example.sh");

        assertThat(script).contains("rclone copy", "--checksum", "--include '/????????-??????/**'");
        assertThat(script).contains(".remote-sync-status", "status=%s", "mv -f --");
        assertThat(script).doesNotContain("rclone sync", "RCLONE_PATH=" + "CHANGE_ME");
    }

    @Test
    void behaviorTest_coversSuccessfulAndFailedRemoteCopies() throws Exception {
        String script = source("deploy/test-sync-backups-rclone.sh");

        assertThat(script).contains("assert_status SUCCESS", "assert_status FAILED");
        assertThat(script).contains("test_remote:paper-mes-backups", "--checksum");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
