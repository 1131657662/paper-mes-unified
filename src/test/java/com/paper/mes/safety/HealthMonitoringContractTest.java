package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HealthMonitoringContractTest {

    @Test
    void actuator_exposesOnlyHealthWithoutDetails() throws Exception {
        String pom = source("pom.xml");
        String application = source("src/main/resources/application.yml");
        String production = source("src/main/resources/application-prod.example.yml");

        assertThat(pom).contains("spring-boot-starter-actuator");
        assertHealthConfiguration(application);
        assertHealthConfiguration(production);
    }

    @Test
    void monitoringScript_checksServiceBackupDiskAndStateTransitions() throws Exception {
        String script = source("deploy/monitor-paper-mes.example.sh");
        String behaviorTest = source("deploy/test-monitor-paper-mes.sh");

        assertThat(script).contains("curl --fail", "actuator/health", "MAX_BACKUP_AGE_HOURS");
        assertThat(script).contains("MIN_BACKUP_FREE_MB", "SHA256SUMS", "ALERT_WEBHOOK_URL");
        assertThat(script).contains("previous_state", "FAILED", "RECOVERED", "STATE_FILE");
        assertThat(script).contains("ALERT_PENDING", "RECOVERY_PENDING", "record_failure", "record_success");
        assertThat(script).contains("--config <(write_webhook_curl_config)");
        assertThat(script).doesNotContain("eval ", "CHANGE_ME", "Bearer ${ALERT_WEBHOOK_BEARER_TOKEN}");
        assertThat(behaviorTest).contains("ALERT_PENDING", "RECOVERY_PENDING", "assert_state UP");
    }

    @Test
    void systemdMonitor_runsAsRestrictedServiceOnTimer() throws Exception {
        String service = source("deploy/paper-mes-monitor.service.example");
        String timer = source("deploy/paper-mes-monitor.timer.example");

        assertThat(service).contains("User=paper-mes", "NoNewPrivileges=true", "ProtectSystem=strict");
        assertThat(service).contains("StateDirectory=paper-mes", "CapabilityBoundingSet=");
        assertThat(timer).contains("OnUnitActiveSec=5min", "Persistent=true");
    }

    @Test
    void sharedServerMonitor_checksAllProjectsInfrastructureAndAlertTransitions() throws Exception {
        String script = source("deploy/monitor-server.example.sh");
        String checks = source("deploy/server-monitor-checks.example.sh");
        String state = source("deploy/server-monitor-state.example.sh");
        String behaviorTest = source("deploy/test-monitor-server.sh");

        assertThat(script).contains("paper-mes.service", "paper-mes-test.service", "pm2-root.service");
        assertThat(script).contains("jimureport", "127.0.0.1:3000", "127.0.0.1:3001");
        assertThat(script).contains("mes.nbsmzwl.cn", "erp.nbsmzwl.cn", "wms.nbsmzwl.cn");
        assertThat(checks).contains("check_mysql", "check_host_resources", "check_certificates", "check_backups");
        assertThat(checks).contains("check_remote_statuses", "check_fresh_files", "tail -n 1");
        assertThat(checks).contains("systemctl is-active");
        assertThat(state).contains("REMINDER_HOURS", "fingerprint", "RECOVERY_PENDING");
        assertThat(behaviorTest).contains("server monitor state transition test passed");
        assertThat(script + checks + state).doesNotContain("CHANGE_ME", "eval ");
    }

    @Test
    void sharedServerMonitor_runsOnTimerWithRestrictedSystemdSettings() throws Exception {
        String service = source("deploy/server-monitor.service.example");
        String timer = source("deploy/server-monitor.timer.example");

        assertThat(service).contains("NoNewPrivileges=true", "ProtectSystem=strict");
        assertThat(service).contains("CapabilityBoundingSet=CAP_DAC_READ_SEARCH");
        assertThat(service).contains("AmbientCapabilities=CAP_DAC_READ_SEARCH");
        assertThat(service).contains("ReadWritePaths=/var/lib/server-monitor");
        assertThat(timer).contains("OnUnitActiveSec=5min", "Persistent=true");
    }

    @Test
    void deploymentGuide_includesMonitorInstallFailureDrillAndRollbackChecks() throws Exception {
        String guide = source("docs/生产部署指南.md");

        assertThat(guide).contains("/actuator/health", "paper-mes-monitor.timer");
        assertThat(guide).contains("FAILED", "RECOVERED", "发布顺序与回滚");
        assertThat(guide).contains("只有 `/usr/local/bin/preflight-paper-mes-release` 通过后才发布前端");
    }

    private void assertHealthConfiguration(String source) {
        assertThat(source).contains("include: health", "show-details: never", "enabled: true");
        assertThat(source).doesNotContain("include: \"*\"", "show-details: always");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
