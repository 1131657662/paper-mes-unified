package com.paper.mes.integration;

import com.paper.mes.delivery.dto.DeliveryCancelDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.service.DeliveryListSummaryService;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.service.SettleListSummaryService;
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
class DocumentSummaryBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryListSummaryService deliverySummaryService;
    @Autowired private SettleService settleService;
    @Autowired private SettleListSummaryService settleSummaryService;

    @Test
    void summaries_whenDocumentsAreVoided_keepCountsButExcludeActiveAmounts() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(deliveryRequest(scenario));
        String settleUuid = settleService.createByOrder(settleRequest(scenario.order().getUuid()));

        var deliveryBefore = deliverySummaryService.summarize(deliveryQuery(scenario.customer().getUuid()));
        var settleBefore = settleSummaryService.summarize(settleQuery(scenario.customer().getUuid()));
        assertThat(deliveryBefore.pendingDocumentCount()).isEqualTo(1);
        assertThat(deliveryBefore.activeWeight()).isEqualByComparingTo("100.000");
        assertThat(settleBefore.pendingDocumentCount()).isEqualTo(1);
        assertThat(settleBefore.activeTotalAmount()).isEqualByComparingTo("100.00");

        deliveryService.cancelPending(deliveryUuid, deliveryCancel());
        settleService.voidSettle(settleUuid, settleVoid());

        var deliveryAfter = deliverySummaryService.summarize(deliveryQuery(scenario.customer().getUuid()));
        var settleAfter = settleSummaryService.summarize(settleQuery(scenario.customer().getUuid()));
        assertThat(deliveryAfter.voidDocumentCount()).isEqualTo(1);
        assertThat(deliveryAfter.activeWeight()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(settleAfter.voidDocumentCount()).isEqualTo(1);
        assertThat(settleAfter.activeTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private DeliveryCreateDTO deliveryRequest(BusinessFlowFixtureFactory.Scenario scenario) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(scenario.first().getUuid());
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(item));
        return request;
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        return SettlementTestRequestFactory.byOrder(settleService, orderUuid);
    }

    private DeliveryQuery deliveryQuery(String customerUuid) {
        DeliveryQuery query = new DeliveryQuery();
        query.setCustomerUuid(customerUuid);
        return query;
    }

    private SettleQuery settleQuery(String customerUuid) {
        SettleQuery query = new SettleQuery();
        query.setCustomerUuid(customerUuid);
        return query;
    }

    private DeliveryCancelDTO deliveryCancel() {
        DeliveryCancelDTO request = new DeliveryCancelDTO();
        request.setReason("summary test void");
        return request;
    }

    private SettleActionReasonDTO settleVoid() {
        SettleActionReasonDTO request = new SettleActionReasonDTO();
        request.setReason("summary test void");
        return request;
    }
}
