package com.paper.mes.integration;

import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.service.SettleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class FullLifecycleBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private DeliveryService deliveryService;
    @Autowired private SettleService settleService;

    @Test
    void standardSaw_whenProductsAndTrimLeaveTogether_completesDeliverySettlementAndReceipt() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        var completed = processOrderService.getDetail(scenario.orderUuid());

        String deliveryUuid = deliveryService.create(deliveryRequest(scenario.customerUuid(), completed));
        deliveryService.confirm(deliveryUuid, confirmRequest());
        String settleUuid = settleService.createByOrder(settleRequest(scenario.orderUuid()));
        var settlement = settleService.getById(settleUuid);
        settleService.receive(settleUuid, receiveRequest(settlement.getTotalAmount()));

        var delivered = processOrderService.getDetail(scenario.orderUuid());
        var received = settleService.getById(settleUuid);
        assertThat(deliveryService.getById(deliveryUuid).getDeliveryStatus()).isEqualTo(2);
        assertThat(delivered.getFinishRolls()).hasSize(3);
        assertThat(delivered.getFinishRolls()).allMatch(item -> item.getRemainingWeight().signum() == 0);
        assertThat(delivered.getFinishRolls()).filteredOn(item -> item.getIsRemain() == 1).hasSize(1);
        assertThat(delivered.getOrder().getOrderStatus()).isEqualTo(5);
        assertThat(received.getSettleStatus()).isEqualTo(3);
        assertThat(received.getReceivedAmount()).isEqualByComparingTo(received.getTotalAmount());
        assertThat(received.getUnreceivedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private DeliveryCreateDTO deliveryRequest(String customerUuid,
                                              com.paper.mes.processorder.dto.ProcessOrderDetailVO detail) {
        DeliveryCreateDTO dto = new DeliveryCreateDTO();
        dto.setCustomerUuid(customerUuid);
        dto.setDeliveryDate(LocalDate.now());
        dto.setPickerName("业务流测试");
        dto.setItems(detail.getFinishRolls().stream().map(this::deliveryItem).toList());
        return dto;
    }

    private DeliveryCreateDTO.Item deliveryItem(FinishRoll finish) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(finish.getUuid());
        item.setOutWeight(finish.getRemainingWeight());
        return item;
    }

    private DeliveryConfirmDTO confirmRequest() {
        DeliveryConfirmDTO dto = new DeliveryConfirmDTO();
        dto.setSignUser("业务流签收人");
        return dto;
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        SettleByOrderDTO dto = new SettleByOrderDTO();
        dto.setOrderUuid(orderUuid);
        dto.setSettleDate(LocalDate.now());
        dto.setIsInvoice(2);
        return dto;
    }

    private ReceiveDTO receiveRequest(BigDecimal amount) {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setReceiveAmount(amount);
        dto.setPayMethod(2);
        dto.setOperator("业务流收款人");
        return dto;
    }
}
