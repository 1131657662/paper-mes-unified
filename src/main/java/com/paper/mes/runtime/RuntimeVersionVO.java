package com.paper.mes.runtime;

public record RuntimeVersionVO(
        String backendVersion,
        String frontendVersion,
        String gitSha,
        String buildTime,
        String databaseVersion,
        String expectedDatabaseVersion,
        boolean databaseReady
) {
}
