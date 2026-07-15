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
    void deploymentGuide_includesMonitorInstallFailureDrillAndRollbackChecks() throws Exception {
        String guide = source("docs/生产部署指南.md");

        assertThat(guide).contains("/actuator/health", "paper-mes-monitor.timer");
        assertThat(guide).contains("FAILED", "RECOVERED", "发布顺序与回滚");
        assertThat(guide).contains("只有 `/actuator/health` 返回 `UP` 后才发布前端");
    }

    private void assertHealthConfiguration(String source) {
        assertThat(source).contains("include: health", "show-details: never", "enabled: true");
        assertThat(source).doesNotContain("include: \"*\"", "show-details: always");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
