package com.paper.mes.integration;

import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleQuoteByOrderDTO;
import com.paper.mes.settle.service.SettleService;

import java.time.LocalDate;
import java.util.UUID;

final class SettlementTestRequestFactory {
    private SettlementTestRequestFactory() {
    }

    static SettleByOrderDTO byOrder(SettleService service, String orderUuid) {
        SettleQuoteByOrderDTO quoteRequest = new SettleQuoteByOrderDTO();
        quoteRequest.setOrderUuid(orderUuid);
        quoteRequest.setIsInvoice(2);
        var quote = service.quoteByOrder(quoteRequest);

        SettleByOrderDTO request = new SettleByOrderDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setQuoteVersion(quote.getQuoteVersion());
        request.setQuoteHash(quote.getQuoteHash());
        request.setOrderUuid(orderUuid);
        request.setSettleDate(LocalDate.now());
        request.setIsInvoice(2);
        return request;
    }
}
