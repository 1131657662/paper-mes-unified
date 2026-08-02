package com.paper.mes.report.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.report.dto.ReportCollectionAnalysisVO;
import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Applies the settlement-read boundary to report response fields. */
@Component
@RequiredArgsConstructor
public class ReportAmountVisibility {

    private final PermissionChecker permissionChecker;

    public void redactOverview(ReportOverviewVO value) {
        if (shouldRedact() && value != null) {
            clearOverview(value);
        }
    }

    public void redactDimensions(List<ReportDimensionVO> rows) {
        if (shouldRedact() && rows != null) {
            rows.forEach(this::clearDimension);
        }
    }

    public void redactDetails(List<ReportDetailVO> rows) {
        if (shouldRedact() && rows != null) {
            rows.forEach(this::clearDetail);
        }
    }

    public void redactSettlement(ReportSettlementAnalysisVO.Overview overview,
                                 List<ReportSettlementAnalysisVO.Dimension> monthly,
                                 List<ReportSettlementAnalysisVO.Dimension> customers) {
        if (!shouldRedact()) return;
        if (overview != null) clearSettlementOverview(overview);
        clearSettlementDimensions(monthly);
        clearSettlementDimensions(customers);
    }

    public void redactCollection(ReportCollectionAnalysisVO.Overview overview,
                                 List<ReportCollectionAnalysisVO.Dimension> monthly,
                                 List<ReportCollectionAnalysisVO.Dimension> customers) {
        if (!shouldRedact()) return;
        if (overview != null) clearCollectionOverview(overview);
        clearCollectionDimensions(monthly);
        clearCollectionDimensions(customers);
    }

    private boolean shouldRedact() {
        return AuthContextHolder.getCurrentUser() != null
                && !permissionChecker.has(Permissions.SETTLE_VIEW);
    }

    private void clearOverview(ReportOverviewVO value) {
        value.setSawAmount(null);
        value.setRewindAmount(null);
        value.setProcessAmount(null);
        value.setExtraAmount(null);
        value.setTotalAmount(null);
        value.setSettledAmount(null);
        value.setPendingSettleAmount(null);
        value.setReceivedAmount(null);
        value.setCashReceivedAmount(null);
        value.setScrapOffsetAmount(null);
        value.setUnreceivedAmount(null);
    }

    private void clearDimension(ReportDimensionVO value) {
        value.setSawAmount(null);
        value.setRewindAmount(null);
        value.setProcessAmount(null);
        value.setExtraAmount(null);
        value.setTotalAmount(null);
        value.setSettledAmount(null);
        value.setPendingSettleAmount(null);
        value.setReceivedAmount(null);
        value.setCashReceivedAmount(null);
        value.setScrapOffsetAmount(null);
        value.setUnreceivedAmount(null);
    }

    private void clearDetail(ReportDetailVO value) {
        value.setSawAmount(null);
        value.setRewindAmount(null);
        value.setProcessAmount(null);
        value.setExtraAmount(null);
        value.setTotalAmount(null);
        value.setSettledAmount(null);
        value.setPendingSettleAmount(null);
        value.setReceivedAmount(null);
        value.setCashReceivedAmount(null);
        value.setScrapOffsetAmount(null);
        value.setUnreceivedAmount(null);
    }

    private void clearSettlementOverview(ReportSettlementAnalysisVO.Overview value) {
        value.setTotalAmount(null);
        value.setReceivedAmount(null);
        value.setUnreceivedAmount(null);
        value.setOverdueAmount(null);
    }

    private void clearSettlementDimensions(List<ReportSettlementAnalysisVO.Dimension> rows) {
        if (rows != null) rows.forEach(value -> {
            value.setTotalAmount(null);
            value.setReceivedAmount(null);
            value.setUnreceivedAmount(null);
        });
    }

    private void clearCollectionOverview(ReportCollectionAnalysisVO.Overview value) {
        value.setSettledAmount(null);
        value.setCashAmount(null);
        value.setScrapOffsetAmount(null);
        value.setDiscountAmount(null);
    }

    private void clearCollectionDimensions(List<ReportCollectionAnalysisVO.Dimension> rows) {
        if (rows != null) rows.forEach(value -> {
            value.setSettledAmount(null);
            value.setCashAmount(null);
            value.setNonCashAmount(null);
        });
    }
}
