package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessFlowTestRunnerContractTest {
    @Test
    void integrationProfile_requiresInjectedDatabaseCredentials() throws Exception {
        String profile = Files.readString(Path.of(
                "src/test/resources/application-business-flow-it.yml"));

        assertThat(profile).contains("${PAPER_MES_IT_DB_URL}",
                "${PAPER_MES_IT_DB_USERNAME}", "${PAPER_MES_IT_DB_PASSWORD}");
        assertThat(profile).doesNotContain("username: root", "PAPER_MES_IT_DB_PASSWORD:");
    }

    @Test
    void runner_preflightsTestDatabaseBeforeMaven() throws Exception {
        String script = Files.readString(Path.of("deploy/run-business-flow-it.ps1"));

        assertThat(script).contains("Refusing non-test database", "TcpClient",
                "Missing $name", "verify -Pbusiness-flow-it");
        assertThat(script).doesNotContain("123123", "--password=");
    }
}
