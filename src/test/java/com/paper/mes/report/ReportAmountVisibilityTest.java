package com.paper.mes.report;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.report.dto.ReportCollectionAnalysisVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import com.paper.mes.report.service.ReportAmountVisibility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAmountVisibilityTest {

    private final ReportAmountVisibility visibility = new ReportAmountVisibility(new PermissionChecker());

    @AfterEach
    void clearAuth() {
        AuthContextHolder.clear();
    }

    @Test
    void operatorReportAmountsAreRedacted() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("operator-1")
                .username("operator").roleCode("operator").build());
        ReportOverviewVO overview = overview();

        visibility.redactOverview(overview);

        assertThat(overview.getTotalAmount()).isNull();
        assertThat(overview.getReceivedAmount()).isNull();
        assertThat(overview.getOriginalWeight()).isEqualByComparingTo("10.000");
    }

    @Test
    void financeReportAmountsRemainVisible() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("finance-1")
                .username("finance").roleCode("finance").build());
        ReportSettlementAnalysisVO.Overview settlement = new ReportSettlementAnalysisVO.Overview();
        settlement.setTotalAmount(new BigDecimal("12.34"));
        ReportCollectionAnalysisVO.Overview collection = new ReportCollectionAnalysisVO.Overview();
        collection.setCashAmount(new BigDecimal("5.67"));

        visibility.redactSettlement(settlement, List.of(), List.of());
        visibility.redactCollection(collection, List.of(), List.of());

        assertThat(settlement.getTotalAmount()).isEqualByComparingTo("12.34");
        assertThat(collection.getCashAmount()).isEqualByComparingTo("5.67");
    }

    private ReportOverviewVO overview() {
        ReportOverviewVO overview = new ReportOverviewVO();
        overview.setTotalAmount(new BigDecimal("12.34"));
        overview.setReceivedAmount(new BigDecimal("5.67"));
        overview.setOriginalWeight(new BigDecimal("10.000"));
        return overview;
    }
}
