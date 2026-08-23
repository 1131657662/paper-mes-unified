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
        assertThat(script).contains("APP_TMP_DIR", "runuser -u \"${APP_USER}\" -- test -w");
        assertThat(script).contains("PrivateTmp", "RuntimeDirectory", "ExecStart", "MainPID");
        assertThat(script).contains("PROC_ROOT", "-Djava.io.tmpdir=${APP_TMP_DIR}");
        assertThat(script).contains("SOURCE_PROVENANCE_SCRIPT", "check_source_provenance");
        assertThat(script).contains(
                "SOURCE_ROOT", "SCHEMA_BASELINE_FILE", "APP_ENV_FILE",
                "PAPER_MES_EXPECTED_SCHEMA_VERSION", "check_schema_version_configuration");
        assertThat(script).contains("duplicate pending finish reservation");
        assertThat(script).contains("duplicate active customer code", "running backup task");
        assertThat(script).doesNotContain("INSERT INTO", "UPDATE `", "DELETE FROM", "DROP TABLE");
    }

    @Test
    void behaviorTestCoversHealthyAndConflictingPreflightResults() throws Exception {
        String script = source("deploy/test-preflight-paper-mes-release.sh");

        assertThat(script).contains(
                "run_preflight 0 0", "run_preflight 1 0", "run_preflight 0 1",
                "schema baseline mismatch");
        assertThat(script).contains("preflight unexpectedly accepted a database conflict");
        assertThat(script).contains("preflight unexpectedly accepted an unwritable application temp directory");
        assertThat(script).contains("preflight unexpectedly accepted PrivateTmp=no");
        assertThat(script).contains("a service unit without the managed temp directory");
        assertThat(script).contains("a running process without the managed temp directory");
        assertThat(script).contains("source files not pulled from GitHub");
    }

    @Test
    void sourceProvenanceRejectsCloudEditsAndUnpulledCommits() throws Exception {
        String verifier = source("deploy/verify-paper-mes-source.example.sh");
        String behaviorTest = source("deploy/test-verify-paper-mes-source.sh");

        assertThat(verifier).contains("status --porcelain", "refs/remotes/${SOURCE_REMOTE}/${SOURCE_BRANCH}");
        assertThat(verifier).contains("installed service unit does not match the pulled source");
        assertThat(verifier).contains("installed release preflight does not match the pulled source");
        assertThat(verifier).contains("installed migration state guard does not match the pulled source");
        assertThat(verifier).contains("installed migration runner does not match the pulled source");
        assertThat(verifier).contains("installed migration lock support does not match the pulled source");
        assertThat(verifier).contains("installed migration state support does not match the pulled source");
        assertThat(behaviorTest).contains("dirty cloud working tree", "commit not pulled from GitHub");
        assertThat(behaviorTest).contains(
                "directly edited service unit", "directly edited preflight",
                "directly edited migration runner");
    }

    @Test
    void deploymentGuideRefreshesRuntimeGuardsAfterEveryPull() throws Exception {
        String guide = source("docs/生产部署指南.md");

        assertThat(guide).contains("每次从 GitHub 拉取新代码后");
        assertThat(guide).contains("必须锁定已经通过测试环境验收的完整 SHA");
        assertThat(guide).contains("deploy-paper-mes.example.sh /usr/local/sbin/deploy-paper-mes.sh");
        assertThat(guide).contains("paper-mes-runtime-rollback.example.sh /usr/local/sbin/paper-mes-runtime-rollback.sh");
        assertThat(guide).contains("禁止在 `/opt/paper-mes/source` 中直接编辑");
        assertThat(guide).contains("bash /opt/paper-mes/source/deploy/backup-paper-mes.example.sh");
        assertThat(guide).contains("deploy/paper-mes.service.example /etc/systemd/system/paper-mes.service");
        assertThat(guide).contains("deploy/preflight-paper-mes-release.example.sh /usr/local/bin/preflight-paper-mes-release");
        assertThat(guide).contains("systemctl daemon-reload", "sudo /usr/local/sbin/deploy-paper-mes.sh");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }
}
