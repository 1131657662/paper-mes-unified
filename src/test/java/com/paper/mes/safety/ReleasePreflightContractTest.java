package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReleasePreflightContractTest {

    @Test
    void preflightChecksHealthBackupIntegrityAndReadOnlyDataConflicts() throws Exception {
        String script = source("deploy/preflight-paper-mes-release.example.sh");

        assertThat(script).contains("actuator/health", "sha256sum -c SHA256SUMS");
        assertThat(script).contains("duplicate pending finish reservation");
        assertThat(script).contains("duplicate active customer code", "running backup task");
        assertThat(script).doesNotContain("INSERT INTO", "UPDATE `", "DELETE FROM", "DROP TABLE");
    }

    @Test
    void behaviorTestCoversHealthyAndConflictingPreflightResults() throws Exception {
        String script = source("deploy/test-preflight-paper-mes-release.sh");

        assertThat(script).contains("run_preflight 0", "run_preflight 1");
        assertThat(script).contains("preflight unexpectedly accepted a database conflict");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
