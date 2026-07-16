package com.paper.mes.integration;

import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.dto.SettleQuoteByOrderDTO;
import com.paper.mes.settle.dto.SettleQuoteByOrdersDTO;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.settle.service.SettleService;
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

    @Test
    void settlement_whenCashIsOneYuanShortAndDiscounted_closesWithoutInflatingCash() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        String settleUuid = settleService.createByOrder(settleRequest(scenario.orderUuid()));
        BigDecimal total = settleService.getById(settleUuid).getTotalAmount();

        AuthContextHolder.setCurrentUser(financeUser());
        try {
            settleService.receive(settleUuid, discountedReceiveRequest(total));
        } finally {
            AuthContextHolder.clear();
        }

        var detail = settleService.getDetail(settleUuid);
        assertThat(detail.getOrder().getSettleStatus()).isEqualTo(3);
        assertThat(detail.getOrder().getReceivedAmount()).isEqualByComparingTo(total);
        assertThat(detail.getOrder().getCashReceivedAmount()).isEqualByComparingTo(total.subtract(BigDecimal.ONE));
        assertThat(detail.getOrder().getDiscountAmount()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(detail.getOrder().getUnreceivedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(detail.getReceives()).singleElement()
                .satisfies(record -> assertThat(record.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ONE));
    }

    @Test
    void settlementQuote_whenCreated_usesExactlyTheQuotedAmounts() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        SettleByOrdersDTO request = new SettleByOrdersDTO();
        request.setOrderUuids(List.of(scenario.orderUuid()));
        request.setSettleDate(LocalDate.now());
        request.setIsInvoice(2);

        SettleQuoteByOrdersDTO quoteRequest = new SettleQuoteByOrdersDTO();
        quoteRequest.setOrderUuids(request.getOrderUuids());
        quoteRequest.setIsInvoice(request.getIsInvoice());
        var quote = settleService.quoteByOrders(quoteRequest);
        request.setRequestId("quote-create-" + System.nanoTime());
        request.setQuoteVersion(quote.getQuoteVersion());
        request.setQuoteHash(quote.getQuoteHash());
        String settleUuid = settleService.createByOrders(request);
        var settlement = settleService.getById(settleUuid);

        assertThat(settlement.getAmountNoTax()).isEqualByComparingTo(quote.getAmountNoTax());
        assertThat(settlement.getTaxAmount()).isEqualByComparingTo(quote.getTaxAmount());
        assertThat(settlement.getTotalAmount()).isEqualByComparingTo(quote.getTotalAmount());
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
        return SettlementTestRequestFactory.byOrder(settleService, orderUuid);
    }

    private ReceiveDTO receiveRequest(BigDecimal amount) {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setRequestId("full-receive-" + System.nanoTime());
        dto.setReceiveAmount(amount);
        dto.setPayMethod(2);
        dto.setPayNo("TX-FULL-1");
        return dto;
    }

    private ReceiveDTO discountedReceiveRequest(BigDecimal amount) {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setRequestId("discount-receive-" + System.nanoTime());
        dto.setCashAmount(amount.subtract(BigDecimal.ONE));
        dto.setDiscountAmount(BigDecimal.ONE);
        dto.setDiscountReason("双方确认尾差");
        dto.setPayMethod(2);
        dto.setPayNo("TX-DISCOUNT-1");
        return dto;
    }

    private CurrentUser financeUser() {
        return CurrentUser.builder().uuid("finance-user").username("finance")
                .realName("业务流财务").roleCode("finance").build();
    }
}
