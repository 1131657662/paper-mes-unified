package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryCancelDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DeliveryEditBusinessFlowIT {

    @Autowired
    private BusinessFlowFixtureFactory fixtures;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private DeliveryDetailMapper deliveryDetailMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void pendingDelivery_whenCancelled_recordsTraceableVoidWithoutHoldingStock() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        assertThat(availableFinishIds(scenario.customer().getUuid()))
                .containsExactlyInAnyOrder(scenario.first().getUuid(), scenario.second().getUuid());

        String deliveryUuid = deliveryService.create(createRequest(scenario));
        DeliveryDetail firstDetail = onlyDetail(deliveryUuid);
        assertDeliveryTotals(deliveryUuid, 1, "100.000");
        assertThat(availableFinishIds(scenario.customer().getUuid()))
                .containsExactly(scenario.second().getUuid());

        deliveryService.removeDetail(deliveryUuid, firstDetail.getUuid());
        assertDeliveryTotals(deliveryUuid, 0, "0.000");
        assertThat(availableFinishIds(scenario.customer().getUuid()))
                .containsExactlyInAnyOrder(scenario.first().getUuid(), scenario.second().getUuid());

        deliveryService.appendDetails(deliveryUuid, appendRequest(scenario.second().getUuid()));
        assertThat(onlyDetail(deliveryUuid).getFinishUuid()).isEqualTo(scenario.second().getUuid());
        assertDeliveryTotals(deliveryUuid, 1, "100.000");

        deliveryService.cancelPending(deliveryUuid, cancelRequest());

        DeliveryOrder voidedOrder = deliveryService.getById(deliveryUuid);
        assertThat(voidedOrder).isNotNull();
        assertThat(voidedOrder.getDeliveryStatus()).isEqualTo(3);
        assertThat(voidedOrder.getVoidReason()).isEqualTo("integration test cancellation");
        assertThat(voidedOrder.getVoidBy()).isEqualTo("system");
        assertThat(voidedOrder.getVoidTime()).isNotNull();
        assertThat(activeLockCount(scenario)).isZero();
        assertThat(availableFinishIds(scenario.customer().getUuid()))
                .containsExactlyInAnyOrder(scenario.first().getUuid(), scenario.second().getUuid());
    }

    private DeliveryCreateDTO createRequest(BusinessFlowFixtureFactory.Scenario scenario) {
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(createItem(scenario.first().getUuid())));
        return request;
    }

    private DeliveryCreateDTO.Item createItem(String finishUuid) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(finishUuid);
        item.setOutWeight(new BigDecimal("100.000"));
        return item;
    }

    private DeliveryAppendItemsDTO appendRequest(String finishUuid) {
        DeliveryAppendItemsDTO.Item item = new DeliveryAppendItemsDTO.Item();
        item.setFinishUuid(finishUuid);
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryAppendItemsDTO request = new DeliveryAppendItemsDTO();
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryCancelDTO cancelRequest() {
        DeliveryCancelDTO request = new DeliveryCancelDTO();
        request.setReason("integration test cancellation");
        return request;
    }

    private Set<String> availableFinishIds(String customerUuid) {
        return deliveryService.listAvailable(customerUuid).stream()
                .map(AvailableFinishVO::getFinishUuid)
                .collect(Collectors.toSet());
    }

    private DeliveryDetail onlyDetail(String deliveryUuid) {
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(
                new LambdaQueryWrapper<DeliveryDetail>().eq(DeliveryDetail::getDeliveryUuid, deliveryUuid));
        assertThat(details).hasSize(1);
        return details.get(0);
    }

    private void assertDeliveryTotals(String uuid, int count, String weight) {
        DeliveryOrder order = deliveryService.getById(uuid);
        assertThat(order.getTotalCount()).isEqualTo(count);
        assertThat(order.getTotalWeight()).isEqualByComparingTo(weight);
    }

    private int activeLockCount(BusinessFlowFixtureFactory.Scenario scenario) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM biz_delivery_detail
                WHERE is_deleted = 0 AND stock_lock_status = 1 AND finish_uuid IN (?, ?)
                """, Integer.class, scenario.first().getUuid(), scenario.second().getUuid());
    }
}
