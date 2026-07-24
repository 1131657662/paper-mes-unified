package com.paper.mes.report.alert;

import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.alert.service.ReportAlertEvaluationDataset;
import com.paper.mes.report.alert.service.ReportAlertEvaluationDatasetLoader;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAlertEvaluationDatasetLoaderTest {
    @Mock
    private ReportService reportService;
    @Mock
    private PaperMapper paperMapper;

    @Test
    void load_allScopeTypes_usesOneQueryPerDimension() {
        when(reportService.overview(any())).thenReturn(overview());
        when(reportService.dimensionSummary(any())).thenAnswer(invocation -> rows(invocation.getArgument(0)));
        when(paperMapper.selectByIds(any(Collection.class))).thenReturn(List.of(paper()));
        var loader = new ReportAlertEvaluationDatasetLoader(reportService, paperMapper);

        ReportAlertEvaluationDataset dataset = loader.load(new ReportQuery(), List.of(
                rule(1, null, null, null), rule(2, "customer-1", null, null),
                rule(3, null, "paper-1", null), rule(4, null, null, 2)));

        assertThat(dataset.snapshots()).containsKeys(
                "GLOBAL", "CUSTOMER:customer-1", "PAPER:paper-1", "PROCESS:2");
        verify(reportService, times(3)).dimensionSummary(any());
    }

    private List<ReportDimensionVO> rows(ReportQuery query) {
        return switch (query.getDimension()) {
            case "customer" -> List.of(row("customer-1", "客户一"));
            case "paper" -> List.of(row("白卡纸", "白卡纸"));
            case "process" -> List.of(row("rewind", "复卷"));
            default -> List.of();
        };
    }

    private ReportOverviewVO overview() {
        ReportOverviewVO value = new ReportOverviewVO();
        value.setOrderCount(1L);
        value.setLossRatio(BigDecimal.ONE);
        return value;
    }

    private ReportDimensionVO row(String key, String name) {
        ReportDimensionVO value = new ReportDimensionVO();
        value.setDimensionKey(key);
        value.setDimensionName(name);
        value.setOrderCount(1L);
        value.setLossRatio(BigDecimal.ONE);
        return value;
    }

    private Paper paper() {
        Paper paper = new Paper();
        paper.setUuid("paper-1");
        paper.setPaperName("白卡纸");
        return paper;
    }

    private ReportAlertRule rule(int scope, String customerUuid, String paperUuid, Integer processType) {
        ReportAlertRule rule = new ReportAlertRule();
        rule.setScopeType(scope);
        rule.setCustomerUuid(customerUuid);
        rule.setPaperUuid(paperUuid);
        rule.setProcessType(processType);
        return rule;
    }
}
