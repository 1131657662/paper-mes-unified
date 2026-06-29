package com.paper.mes.report.service.impl;

import com.paper.mes.report.dto.CustomerReportVO;
import com.paper.mes.report.dto.LossReportVO;
import com.paper.mes.report.dto.MachineReportVO;
import com.paper.mes.report.dto.MonthlyReportVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportMapper;
import com.paper.mes.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    @Override
    public List<MonthlyReportVO> monthlySummary(ReportQuery query) {
        return reportMapper.monthlySummary(query);
    }

    @Override
    public List<CustomerReportVO> customerSummary(ReportQuery query) {
        return reportMapper.customerSummary(query);
    }

    @Override
    public List<LossReportVO> lossAnalysis(ReportQuery query) {
        return reportMapper.lossAnalysis(query);
    }

    @Override
    public List<MachineReportVO> machineOutput(ReportQuery query) {
        return reportMapper.machineOutput(query);
    }
}
