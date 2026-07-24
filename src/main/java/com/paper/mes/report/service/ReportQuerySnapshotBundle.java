package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;

/** Query plus the metric release locked when the snapshot was created. */
public record ReportQuerySnapshotBundle(ReportQuery query, ReportQuerySnapshotVO snapshot) {
}
