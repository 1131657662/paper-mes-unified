package com.paper.mes.report.service;

record ReportQuerySnapshotLookup(
        String ownerUuid,
        String permissionHash,
        String queryHash,
        String metricReleaseUuid,
        long idempotencyBucket
) {
}
