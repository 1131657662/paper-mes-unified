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

    @Test
    void retentionScript_defaultsToDryRunAndUsesBoundedHardDeletes() throws Exception {
        String script = source("deploy/prune-offsite-backups.example.sh");

        assertThat(script).contains("OFFSITE_RETENTION_APPLY:-false", "MAX_DELETE_COUNT:-50");
        assertThat(script).contains("rclone purge", "--b2-hard-delete", "refusing to delete newest backup");
        assertThat(script).doesNotContain("rclone sync");
    }

    @Test
    void capacityScript_checksB2VersionsAndTransitionThresholds() throws Exception {
        String script = source("deploy/check-offsite-backup-capacity.example.sh");

        assertThat(script).contains("WARNING_BYTES:-8000000000", "CRITICAL_BYTES:-9000000000");
        assertThat(script).contains("--b2-versions", "previous_level", "RECOVERED");
    }

    @Test
    void offsiteBehaviorTestsCoverRetentionCapacityAndOrchestration() throws Exception {
        assertThat(source("deploy/test-prune-offsite-backups.sh"))
                .contains("monthly-permanent", "MAX_DELETE_COUNT", "unexpected-name");
        assertThat(source("deploy/test-check-offsite-backup-capacity.sh"))
                .contains("WARNING", "CRITICAL", "RECOVERED", "--b2-versions");
        assertThat(source("deploy/test-sync-offsite-backups.sh"))
                .contains("business mes retention capacity", "email:FAILED", "email:RECOVERED");
    }

    @Test
    void systemdTemplatesEnableRetentionAfterLocalCleanup() throws Exception {
        String service = source("deploy/paper-mes-offsite-backup.service.example");
        String timer = source("deploy/paper-mes-offsite-backup.timer.example");

        assertThat(service).contains("OFFSITE_RETENTION_APPLY=true", "DAILY_RETENTION_YEARS=3");
        assertThat(service).contains("WEEKLY_RETENTION_YEARS=5", "MAX_DELETE_COUNT=50");
        assertThat(service).contains("WARNING_BYTES=8000000000", "CRITICAL_BYTES=9000000000");
        assertThat(timer).contains("OnCalendar=*-*-* 03:30:00 Asia/Shanghai", "Persistent=true");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
