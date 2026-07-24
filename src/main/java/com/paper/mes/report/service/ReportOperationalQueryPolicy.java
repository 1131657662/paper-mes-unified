package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQuery;
import org.springframework.stereotype.Component;

@Component
public class ReportOperationalQueryPolicy {

    public void requireSettlement(ReportQuery query) {
        requireNoProductionFilters(query);
    }

    public void requireCollection(ReportQuery query) {
        requireNoProductionFilters(query);
    }

    public void requireInventory(ReportQuery query) {
        requireNoProcessFilters(query);
        if (query.getSettleType() != null || query.getIsInvoice() != null) reject();
    }

    public void requireDelivery(ReportQuery query) {
        requireInventory(query);
        if (hasText(query.getPaperName())) reject();
    }

    private void requireNoProductionFilters(ReportQuery query) {
        requireNoProcessFilters(query);
        if (hasText(query.getPaperName())) reject();
    }

    private void requireNoProcessFilters(ReportQuery query) {
        if (query.getMainStepType() != null || query.getProcessMode() != null
                || hasText(query.getMachineUuid()) || query.getOrderStatus() != null
                || hasText(query.getDimension())) reject();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void reject() {
        throw new BusinessException("当前专题不支持所提交的筛选条件");
    }
}
