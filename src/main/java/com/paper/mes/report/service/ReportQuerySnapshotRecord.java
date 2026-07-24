package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;

record ReportQuerySnapshotRecord(
        String ownerUuid,
        String permissionHash,
        ReportQuery query,
        ReportQuerySnapshotVO snapshot
) {
}
