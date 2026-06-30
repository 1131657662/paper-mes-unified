package com.paper.mes.report.service;

import com.paper.mes.report.dto.CustomerReportVO;
import com.paper.mes.report.dto.LossReportVO;
import com.paper.mes.report.dto.MachineReportVO;
import com.paper.mes.report.dto.MonthlyReportVO;
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

    List<MonthlyReportVO> monthlySummary(ReportQuery query);

    List<CustomerReportVO> customerSummary(ReportQuery query);

    List<LossReportVO> lossAnalysis(ReportQuery query);

    List<MachineReportVO> machineOutput(ReportQuery query);
}
