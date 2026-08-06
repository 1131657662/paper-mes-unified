package com.paper.mes.runtime;

import java.time.Instant;
import java.util.List;

public record SchemaReadinessReport(
        boolean ready,
        String databaseVersion,
        String expectedVersion,
        List<String> missingStructures,
        Instant checkedAt
) {
}
