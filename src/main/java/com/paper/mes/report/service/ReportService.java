package com.paper.mes.report.service;

import com.paper.mes.common.PageResult;
import com.paper.mes.report.dto.ReportDetailQuery;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportProductionAnalysisVO;
import com.paper.mes.report.dto.ReportQualityLossAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportExportExecutionRequest;

import java.util.List;
import java.nio.file.Path;

/**
 * 统计报表：月度汇总 / 客户维度 / 损耗分析 / 机台产出。
 */
public interface ReportService {

    ReportOverviewVO overview(ReportQuery query);

    ReportProductionAnalysisVO productionAnalysis(ReportQuery query);

    ReportQualityLossAnalysisVO qualityLossAnalysis(ReportQuery query);

    List<ReportDimensionVO> dimensionSummary(ReportQuery query);

    PageResult<ReportDetailVO> detailRows(ReportDetailQuery query);

    List<String> paperCandidates(String keyword);

    void exportWorkbook(ReportQuery query, Path target);

    void exportWorkbook(ReportExportExecutionRequest request, Path target);
}
