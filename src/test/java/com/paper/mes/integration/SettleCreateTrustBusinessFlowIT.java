package com.paper.mes.integration;

import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.dto.SettleQuoteByOrderDTO;
import com.paper.mes.settle.dto.SettleQuoteByOrdersDTO;
import com.paper.mes.settle.service.SettleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SettleCreateTrustBusinessFlowIT {
    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private SettleService settleService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void createByOrder_whenRequestRepeats_returnsOriginalSettlement() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        SettleByOrderDTO request = SettlementTestRequestFactory.byOrder(
                settleService, scenario.order().getUuid());

        String first = settleService.createByOrder(request);
        String second = settleService.createByOrder(request);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void createByOrder_whenQuoteChanges_rejectsStaleQuote() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        SettleByOrderDTO request = SettlementTestRequestFactory.byOrder(
                settleService, scenario.order().getUuid());
        jdbcTemplate.update("UPDATE biz_process_order SET total_amount_no_tax = 110 WHERE uuid = ?",
                scenario.order().getUuid());

        assertThatThrownBy(() -> settleService.createByOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("报价已变化");
    }

    @Test
    void createByOrders_withMultipleOrders_recordsSelectedMergeType() {
        var first = fixtures.createCompletedOrderWithTwoFinishes();
        var second = fixtures.createCompletedOrderForCustomer(first.customer());
        List<String> orderUuids = List.of(first.order().getUuid(), second.order().getUuid());
        SettleByOrdersDTO request = selectedMergeRequest(orderUuids);

        String uuid = settleService.createByOrders(request);

        assertThat(settleService.getById(uuid).getSettleType()).isEqualTo(3);
    }

    private SettleByOrdersDTO selectedMergeRequest(List<String> orderUuids) {
        SettleQuoteByOrdersDTO quoteRequest = new SettleQuoteByOrdersDTO();
        quoteRequest.setOrderUuids(orderUuids);
        quoteRequest.setIsInvoice(2);
        var quote = settleService.quoteByOrders(quoteRequest);
        SettleByOrdersDTO request = new SettleByOrdersDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setQuoteVersion(quote.getQuoteVersion());
        request.setQuoteHash(quote.getQuoteHash());
        request.setOrderUuids(orderUuids);
        request.setSettleDate(LocalDate.now());
        request.setIsInvoice(2);
        return request;
    }
}
