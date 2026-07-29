package com.paper.mes.integration;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryPendingUpdateDTO;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.service.DeliveryPendingHeaderService;
import com.paper.mes.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DeliveryPendingHeaderBusinessFlowIT extends AuthenticatedBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryPendingHeaderService pendingHeaderService;

    @Test
    void pendingDelivery_whenVehicleBecomesKnown_updatesHeaderInformation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));

        pendingHeaderService.update(deliveryUuid, updateRequest(" 浙A12345 "));

        DeliveryOrder updated = deliveryService.getById(deliveryUuid);
        assertThat(updated.getDeliveryStatus()).isEqualTo(1);
        assertThat(updated.getReceiverCustomerName()).isEqualTo("永丰包装");
        assertThat(updated.getCarNo()).isEqualTo("浙A12345");
        assertThat(updated.getContainerNo()).isEqualTo("GX-08");
        assertThat(updated.getPickerName()).isNull();
    }

    @Test
    void confirmedDelivery_whenHeaderUpdateRequested_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());

        assertThatThrownBy(() -> pendingHeaderService.update(
                deliveryUuid, updateRequest("浙A12345")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待出库单允许编辑");
    }

    @Test
    void rolledBackDelivery_whenVehicleBecomesKnown_allowsHeaderUpdate() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());
        deliveryService.rollback(deliveryUuid, rollbackRequest());

        pendingHeaderService.update(deliveryUuid, updateRequest("浙B67890"));

        DeliveryOrder updated = deliveryService.getById(deliveryUuid);
        assertThat(updated.getDeliveryStatus()).isEqualTo(1);
        assertThat(updated.getCarNo()).isEqualTo("浙B67890");
    }

    private DeliveryCreateDTO createRequest(BusinessFlowFixtureFactory.Scenario scenario) {
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setWarehouseUuid(scenario.order().getWarehouseUuid());
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

    private DeliveryPendingUpdateDTO updateRequest(String carNo) {
        DeliveryPendingUpdateDTO request = new DeliveryPendingUpdateDTO();
        request.setReceiverCustomerName(" 永丰包装 ");
        request.setDeliveryDate(LocalDate.now().plusDays(1));
        request.setPickerName("   ");
        request.setCarNo(carNo);
        request.setContainerNo(" GX-08 ");
        request.setRemark(" 等待车辆到厂 ");
        return request;
    }

    private DeliveryRollbackDTO rollbackRequest() {
        DeliveryRollbackDTO request = new DeliveryRollbackDTO();
        request.setReason("车辆信息需要调整");
        return request;
    }
}
