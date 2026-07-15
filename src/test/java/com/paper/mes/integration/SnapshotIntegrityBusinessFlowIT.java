package com.paper.mes.integration;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.health.service.DataHealthService;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.service.SettleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SnapshotIntegrityBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;
    @Autowired private SettleService settleService;
    @Autowired private DataHealthService dataHealthService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void corruptedDeliverySnapshot_isBlockedAndReportedAsCritical() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = createConfirmedDelivery(scenario);
        jdbcTemplate.update("UPDATE biz_delivery_order SET snap_delivery = '{}' WHERE uuid = ?", deliveryUuid);

        assertThatThrownBy(() -> deliveryService.getDetail(deliveryUuid))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "E008");

        assertThat(dataHealthService.inspect().issues()).anyMatch(issue ->
                deliveryUuid.equals(issue.businessUuid())
                        && "DELIVERY_SNAPSHOT_CORRUPTED".equals(issue.issueType())
                        && "CRITICAL".equals(issue.severity()));
    }

    @Test
    void corruptedSettlementSnapshot_isBlockedAndReportedAsCritical() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String settleUuid = settleService.createByOrder(settleRequest(scenario.order().getUuid()));
        jdbcTemplate.update("UPDATE biz_settle_order SET snap_bill = '{}' WHERE uuid = ?", settleUuid);

        assertThatThrownBy(() -> settleService.getDetail(settleUuid))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "E008");

        assertThat(dataHealthService.inspect().issues()).anyMatch(issue ->
                settleUuid.equals(issue.businessUuid())
                        && "SETTLEMENT_SNAPSHOT_CORRUPTED".equals(issue.issueType())
                        && "CRITICAL".equals(issue.severity()));
    }

    @Test
    void confirmedDelivery_whenLiveRowsChange_keepsFrozenDetailValues() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = createConfirmedDelivery(scenario);
        DeliveryDetailVO before = deliveryService.getDetail(deliveryUuid);
        jdbcTemplate.update("UPDATE biz_delivery_detail SET out_weight = 1 WHERE delivery_uuid = ?", deliveryUuid);
        jdbcTemplate.update("UPDATE biz_finish_roll SET paper_name = 'changed' WHERE uuid = ?",
                scenario.first().getUuid());

        DeliveryDetailVO after = deliveryService.getDetail(deliveryUuid);

        assertThat(after.getDetails().getFirst().getOutWeight())
                .isEqualByComparingTo(before.getDetails().getFirst().getOutWeight());
        assertThat(after.getDetails().getFirst().getPaperName())
                .isEqualTo(before.getDetails().getFirst().getPaperName());
    }

    @Test
    void settlement_whenLiveRowsChange_keepsFrozenDetailAndPrintValues() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        addOriginalRoll(scenario.order().getUuid());
        String settleUuid = settleService.createByOrder(settleRequest(scenario.order().getUuid()));
        SettleDetailVO before = settleService.getDetail(settleUuid);
        jdbcTemplate.update("UPDATE biz_settle_detail SET order_amount = 1 WHERE settle_uuid = ?", settleUuid);
        jdbcTemplate.update("UPDATE biz_process_order SET total_amount = 1 WHERE uuid = ?", scenario.order().getUuid());

        SettleDetailVO after = settleService.getDetail(settleUuid);

        assertThat(after.getDetails().getFirst().getOrderAmount())
                .isEqualByComparingTo(before.getDetails().getFirst().getOrderAmount());
        assertThat(after.getPrintLines().getFirst().getLineAmount())
                .isEqualByComparingTo(before.getPrintLines().getFirst().getLineAmount());
    }

    private String createConfirmedDelivery(BusinessFlowFixtureFactory.Scenario scenario) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(scenario.first().getUuid());
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(item));
        String uuid = deliveryService.create(request);
        deliveryService.confirm(uuid, new DeliveryConfirmDTO());
        return uuid;
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        SettleByOrderDTO request = new SettleByOrderDTO();
        request.setOrderUuid(orderUuid);
        request.setSettleDate(LocalDate.now());
        request.setIsInvoice(2);
        return request;
    }

    private void addOriginalRoll(String orderUuid) {
        String originalUuid = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO biz_original_roll
                  (uuid, order_uuid, row_sort, paper_name, gram_weight, original_width,
                   roll_weight, piece_num, total_weight, process_mode, main_step_type, roll_status)
                VALUES (?, ?, 1, 'snapshot-paper', 80, 1200, 200, 1, 200, 1, 1, 3)
                """, originalUuid, orderUuid);
        jdbcTemplate.update("UPDATE biz_process_step SET original_uuid = ? WHERE order_uuid = ?",
                originalUuid, orderUuid);
    }
}
