package com.paper.mes.integration;

import com.paper.mes.delivery.dto.DeliveryInventoryFilter;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.mapper.ReportOperationalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ReportInventoryBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryInventoryMapper deliveryInventoryMapper;
    @Autowired private ReportOperationalMapper reportOperationalMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void inventoryReport_whenStoredRollIsExhausted_matchesDeliveryInventory() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        jdbcTemplate.update("UPDATE biz_finish_roll SET remaining_weight = 0 WHERE uuid = ?",
                scenario.first().getUuid());

        DeliveryInventoryFilter inventoryFilter = new DeliveryInventoryFilter();
        inventoryFilter.setCustomerUuid(scenario.customer().getUuid());
        ReportQuery reportQuery = new ReportQuery();
        reportQuery.setCustomerUuid(scenario.customer().getUuid());

        var inventory = deliveryInventoryMapper.summary(inventoryFilter);
        var report = reportOperationalMapper.inventoryOverview(reportQuery);

        assertThat(report.getRollCount()).isEqualTo(inventory.getTotalRollCount()).isEqualTo(1);
        assertThat(report.getTotalWeight()).isEqualByComparingTo(inventory.getTotalWeight());
    }
}
