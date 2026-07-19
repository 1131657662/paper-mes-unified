package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportDetailsVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;

import java.util.List;
import java.nio.file.Path;

/**
 * 统计报表：月度汇总 / 客户维度 / 损耗分析 / 机台产出。
 */
public interface ReportService {

    ReportOverviewVO overview(ReportQuery query);

    List<ReportDimensionVO> dimensionSummary(ReportQuery query);

    ReportDetailsVO detailRows(ReportQuery query);

    void exportWorkbook(ReportQuery query, Path target);
}
