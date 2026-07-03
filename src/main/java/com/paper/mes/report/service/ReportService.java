package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 统计报表：月度汇总 / 客户维度 / 损耗分析 / 机台产出。
 */
public interface ReportService {

    ReportOverviewVO overview(ReportQuery query);

    List<ReportDimensionVO> dimensionSummary(ReportQuery query);

    List<ReportDetailVO> detailRows(ReportQuery query);

    void exportWorkbook(ReportQuery query, HttpServletResponse response);
}
