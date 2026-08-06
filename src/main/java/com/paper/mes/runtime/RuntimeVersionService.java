package com.paper.mes.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RuntimeVersionService {

    private final SchemaReadinessService readinessService;
    private final String backendVersion;
    private final String frontendVersion;
    private final String gitSha;
    private final String buildTime;

    public RuntimeVersionService(
            SchemaReadinessService readinessService,
            @Value("${app.runtime.backend-version:dev}") String backendVersion,
            @Value("${app.runtime.frontend-version:dev}") String frontendVersion,
            @Value("${app.runtime.git-sha:unknown}") String gitSha,
            @Value("${app.runtime.build-time:unknown}") String buildTime) {
        this.readinessService = readinessService;
        this.backendVersion = backendVersion;
        this.frontendVersion = frontendVersion;
        this.gitSha = gitSha;
        this.buildTime = buildTime;
    }

    public RuntimeVersionVO current() {
        SchemaReadinessReport schema = readinessService.refresh();
        return new RuntimeVersionVO(backendVersion, frontendVersion, gitSha, buildTime,
                schema.databaseVersion(), schema.expectedVersion(), schema.ready());
    }
}
