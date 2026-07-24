package com.paper.mes.integration;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProcessOrderPrintConcurrencyBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper processOrderMapper;

    private BackRecordOnSiteFixture.Scenario scenario;

    @BeforeEach
    void setUp() {
        scenario = fixture.arrange();
        scenario.order().setOrderStatus(1);
        scenario.order().setPrintStatus(0);
        scenario.order().setPrintCount(0);
        processOrderMapper.updateById(scenario.order());
    }

    @AfterEach
    void tearDown() {
        cleanup.delete(scenario.order().getUuid());
    }

    @Test
    void issue_whenSubmittedConcurrently_issuesExactlyOnceWithoutPrinting() throws Exception {
        String orderUuid = scenario.order().getUuid();

        var outcomes = ConcurrentBusinessActions.<PrintResultVO>runPair(
                () -> processOrderService.issue(orderUuid),
                () -> processOrderService.issue(orderUuid));

        assertThat(outcomes).filteredOn(ConcurrentBusinessActions.Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded())
                .extracting(ConcurrentBusinessActions.Outcome::error)
                .allMatch(BusinessException.class::isInstance);
        var stored = processOrderMapper.selectById(orderUuid);
        assertThat(stored.getOrderStatus()).isEqualTo(2);
        assertThat(stored.getPrintCount()).isEqualTo(0);
        assertThat(stored.getPrintStatus()).isEqualTo(0);
        assertThat(stored.getSnapPrint()).contains("\"print_count\": 0");
    }

    @Test
    void print_whenSubmittedConcurrentlyAfterIssue_recordsOnePhysicalPrint() throws Exception {
        String orderUuid = scenario.order().getUuid();
        processOrderService.issue(orderUuid);

        var outcomes = ConcurrentBusinessActions.<PrintResultVO>runPair(
                () -> processOrderService.print(orderUuid, new PrintDTO()),
                () -> processOrderService.print(orderUuid, new PrintDTO()));

        assertThat(outcomes).filteredOn(ConcurrentBusinessActions.Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded())
                .extracting(ConcurrentBusinessActions.Outcome::error)
                .allMatch(BusinessException.class::isInstance);
        var stored = processOrderMapper.selectById(orderUuid);
        assertThat(stored.getOrderStatus()).isEqualTo(2);
        assertThat(stored.getPrintCount()).isEqualTo(1);
        assertThat(stored.getPrintStatus()).isEqualTo(1);
        assertThat(stored.getSnapPrint()).contains("\"print_count\": 0");
    }
}
