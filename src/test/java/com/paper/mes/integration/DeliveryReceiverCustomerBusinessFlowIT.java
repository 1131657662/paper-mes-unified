package com.paper.mes.integration;

import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DeliveryReceiverCustomerBusinessFlowIT extends AuthenticatedBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;

    @Test
    void create_whenReceiverCustomerProvided_persistsTrimmedName() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        DeliveryCreateDTO request = createRequest(scenario, "  永丰包装  ");

        String deliveryUuid = deliveryService.create(request);

        assertThat(deliveryService.getById(deliveryUuid).getReceiverCustomerName())
                .isEqualTo("永丰包装");
    }

    @Test
    void create_whenReceiverCustomerBlank_persistsNull() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        DeliveryCreateDTO request = createRequest(scenario, "   ");

        String deliveryUuid = deliveryService.create(request);

        assertThat(deliveryService.getById(deliveryUuid).getReceiverCustomerName()).isNull();
    }

    private DeliveryCreateDTO createRequest(BusinessFlowFixtureFactory.Scenario scenario,
                                             String receiverCustomerName) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(scenario.first().getUuid());
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setReceiverCustomerName(receiverCustomerName);
        request.setWarehouseUuid(scenario.order().getWarehouseUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(item));
        return request;
    }
}
