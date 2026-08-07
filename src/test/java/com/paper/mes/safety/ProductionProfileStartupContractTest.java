package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionProfileStartupContractTest {

    @Test
    void smokeScriptUsesIsolatedDatabaseAndVerifiesProductionSecurityBoundaries() throws Exception {
        String script = source("deploy/test-production-profile-startup.ps1");

        assertThat(script).contains("paper_processing_prod_smoke_test", "refusing to overwrite it");
        assertThat(script).contains("Apply-Schema", "Apply-PendingMigrations", "V*.sql");
        assertThat(script).contains("execution_type", "status='running'", "Get-FileHash");
        assertThat(script).doesNotContain("Register-MigrationBaseline", "'baseline'");
        assertThat(script).contains("SPRING_PROFILES_ACTIVE = \"prod\"");
        assertThat(script).contains(
                "PAPER_MES_EXPECTED_SCHEMA_VERSION = $currentBaselineVersion",
                "PAPER_MES_BACKEND_VERSION = \"prod-smoke-backend\"",
                "PAPER_MES_FRONTEND_VERSION = \"prod-smoke-frontend\"",
                "PAPER_MES_GIT_SHA", "PAPER_MES_BUILD_TIME");
        assertThat(script).contains("actuator/env", "api/auth/me", "Set-Cookie");
        assertThat(script).contains("Secure", "HttpOnly", "SameSite=Strict", "DROP DATABASE");
    }

    @Test
    void productionConfigBindsToLoopbackAndHidesHealthDetails() throws Exception {
        String config = source("src/main/resources/application-prod.example.yml");

        assertThat(config).contains("address: 127.0.0.1", "include: health", "show-details: never");
        assertThat(config).contains("cookie-secure: true", "schema-bootstrap:", "enabled: false");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
