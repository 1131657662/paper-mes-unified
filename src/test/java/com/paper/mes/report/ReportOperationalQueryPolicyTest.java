package com.paper.mes.report;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportOperationalQueryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportOperationalQueryPolicyTest {
    private final ReportOperationalQueryPolicy policy = new ReportOperationalQueryPolicy();

    @Test
    void settlement_acceptsOnlyFinancialAndCommonFilters() {
        ReportQuery valid = new ReportQuery();
        valid.setCustomerUuid("customer");
        valid.setSettleType(2);
        valid.setIsInvoice(1);
        assertDoesNotThrow(() -> policy.requireSettlement(valid));

        valid.setPaperName("牛卡纸");
        assertThrows(BusinessException.class, () -> policy.requireSettlement(valid));
    }

    @Test
    void inventory_rejectsFinancialOrProcessFilters() {
        ReportQuery valid = new ReportQuery();
        valid.setPaperName("牛卡纸");
        assertDoesNotThrow(() -> policy.requireInventory(valid));

        valid.setSettleType(1);
        assertThrows(BusinessException.class, () -> policy.requireInventory(valid));
    }

    @Test
    void delivery_rejectsPaperFilter() {
        ReportQuery query = new ReportQuery();
        query.setPaperName("牛卡纸");
        assertThrows(BusinessException.class, () -> policy.requireDelivery(query));
    }
}
