package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;
import org.springframework.stereotype.Component;

@Component
public class ReportExportConsistencyGuard {
    public void requireSameMetricContext(ReportQuerySnapshotVO submission,
                                         ReportQueryExecutionMetaVO execution) {
        if (submission == null || !execution.metricReleaseUuid().equals(submission.metricReleaseUuid())
                || !execution.metricVersionMap().equals(submission.metricVersionMap())) {
            throw new BusinessException("指标口径在导出执行前发生变化，请重新发起导出");
        }
    }
}
