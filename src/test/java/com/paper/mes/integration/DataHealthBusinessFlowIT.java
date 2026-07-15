package com.paper.mes.integration;

import com.paper.mes.health.dto.DataHealthRepairRequest;
import com.paper.mes.health.dto.DataHealthSummaryVO;
import com.paper.mes.health.service.DataHealthRepairService;
import com.paper.mes.health.service.DataHealthService;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.service.SettleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DataHealthBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DataHealthService dataHealthService;
    @Autowired private DataHealthRepairService repairService;
    @Autowired private SettleService settleService;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void settlementTotalMismatch_whenReconciled_restoresDetailTotal() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String settleUuid = settleService.createByOrder(settleRequest(scenario.order().getUuid()));
        String settleNo = settleService.getById(settleUuid).getSettleNo();
        jdbcTemplate.update("UPDATE biz_settle_order SET total_amount = 90 WHERE uuid = ?", settleUuid);

        DataHealthSummaryVO before = dataHealthService.inspect();
        assertThat(before.issues()).anyMatch(issue -> settleUuid.equals(issue.businessUuid())
                && "SETTLEMENT_TOTAL_MISMATCH".equals(issue.issueType()));

        repairService.reconcileSettlement(settleUuid, request(settleNo));

        assertThat(settleService.getById(settleUuid).getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(dataHealthService.inspect().issues()).noneMatch(issue -> settleUuid.equals(issue.businessUuid())
                && "SETTLEMENT_TOTAL_MISMATCH".equals(issue.issueType()));
    }

    @Test
    void settledOrderWithoutSettlement_whenRestored_returnsToCompleted() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        jdbcTemplate.update("UPDATE biz_process_order SET order_status = 5 WHERE uuid = ?", scenario.order().getUuid());

        assertThat(dataHealthService.inspect().issues()).anyMatch(issue -> scenario.order().getUuid().equals(issue.businessUuid())
                && "SETTLED_ORDER_WITHOUT_SETTLEMENT".equals(issue.issueType()));

        repairService.restoreCompletedOrder(scenario.order().getUuid(), request(scenario.order().getOrderNo()));

        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(4);
    }

    @Test
    void overdueWorkflowItems_areReportedAsWarnings() {
        BusinessFlowFixtureFactory.Scenario backRecord = fixtures.createCompletedOrderWithTwoFinishes();
        jdbcTemplate.update("UPDATE biz_process_order SET order_status = 3, update_time = TIMESTAMPADD(DAY, -4, NOW()) WHERE uuid = ?",
                backRecord.order().getUuid());
        BusinessFlowFixtureFactory.Scenario receivable = fixtures.createCompletedOrderWithTwoFinishes();
        String settleUuid = settleService.createByOrder(settleRequest(receivable.order().getUuid()));
        jdbcTemplate.update("UPDATE biz_settle_order SET settle_status = 1, unreceived_amount = 100, settle_date = TIMESTAMPADD(DAY, -31, CURDATE()) WHERE uuid = ?",
                settleUuid);

        DataHealthSummaryVO summary = dataHealthService.inspect();

        assertThat(summary.issues()).anyMatch(issue -> backRecord.order().getUuid().equals(issue.businessUuid())
                && "OVERDUE_BACK_RECORD".equals(issue.issueType()) && "WARNING".equals(issue.severity()));
        assertThat(summary.issues()).anyMatch(issue -> settleUuid.equals(issue.businessUuid())
                && "OVERDUE_RECEIVABLE".equals(issue.issueType()) && "WARNING".equals(issue.severity()));
    }

    @Test
    void inStockOnSiteFinishWithoutWidth_isReportedAsCritical() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String originalUuid = java.util.UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO biz_original_roll
                  (uuid, order_uuid, row_sort, paper_name, gram_weight, original_width,
                   roll_weight, piece_num, total_weight, process_mode, main_step_type, roll_status)
                VALUES (?, ?, 1, 'health-paper', 80, 1200, 200, 1, 200, 2, 1, 3)
                """, originalUuid, scenario.order().getUuid());
        jdbcTemplate.update("""
                INSERT INTO biz_finish_original_rel
                  (uuid, order_uuid, finish_uuid, original_uuid, share_ratio)
                VALUES (?, ?, ?, ?, 100)
                """, java.util.UUID.randomUUID().toString().replace("-", ""),
                scenario.order().getUuid(), scenario.first().getUuid(), originalUuid);
        jdbcTemplate.update("UPDATE biz_finish_roll SET finish_width = 0 WHERE uuid = ?", scenario.first().getUuid());

        DataHealthSummaryVO summary = dataHealthService.inspect();

        assertThat(summary.issues()).anyMatch(issue -> scenario.first().getUuid().equals(issue.businessUuid())
                && "ONSITE_FINISH_WIDTH_INVALID".equals(issue.issueType())
                && "CRITICAL".equals(issue.severity()));
    }

    @Test
    void completedOrderWithMissingProductionActuals_isReportedAsCritical() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        jdbcTemplate.update("""
                UPDATE biz_finish_roll
                SET actual_weight = NULL, remaining_weight = NULL, finish_status = 1
                WHERE order_uuid = ?
                """, scenario.order().getUuid());

        DataHealthSummaryVO summary = dataHealthService.inspect();

        assertThat(summary.issues()).filteredOn(issue -> scenario.order().getUuid().equals(issue.businessUuid()))
                .extracting(issue -> issue.issueType())
                .contains("COMPLETED_ORIGINAL_WEIGHT_MISSING",
                        "COMPLETED_FINISH_WEIGHT_MISSING",
                        "COMPLETED_FINISH_PENDING_INBOUND",
                        "COMPLETED_ORDER_WITHOUT_ACTUAL_OUTPUT");
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        SettleByOrderDTO request = new SettleByOrderDTO();
        request.setOrderUuid(orderUuid);
        request.setSettleDate(LocalDate.now());
        request.setIsInvoice(2);
        return request;
    }

    private DataHealthRepairRequest request(String confirmation) {
        return new DataHealthRepairRequest("integration health repair", confirmation);
    }
}
