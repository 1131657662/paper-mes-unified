package com.paper.mes.remain.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.dto.RemainSaleCreateDTO;
import com.paper.mes.remain.dto.RemainSaleLineDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RemainSalePolicyTest {

    @Test
    void validateRequest_withTotalAmountPricingAndMissingAmount_rejectsInput() {
        RemainSaleCreateDTO request = new RemainSaleCreateDTO();
        request.setRequestId("sale-request");
        request.setProcessDate(LocalDateTime.now());
        request.setPricingMode(RemainSalePolicy.TOTAL_AMOUNT);
        request.setReceivedAmount(BigDecimal.ZERO);
        RemainSaleLineDTO line = new RemainSaleLineDTO();
        line.setLotUuid("lot-1");
        line.setSystemWeight(new BigDecimal("1"));
        request.setLines(List.of(line));

        assertThrows(BusinessException.class, () -> RemainSalePolicy.validateRequest(request));
    }
}
