package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
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
class SettleReceiveIdempotencyBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private SettleService settleService;
    @Autowired private ReceiveRecordMapper receiveRecordMapper;

    @Test
    void receive_whenRequestIdRepeats_recordsPaymentOnce() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        String settleUuid = settleService.createByOrder(settleRequest(scenario.orderUuid()));
        BigDecimal total = settleService.getById(settleUuid).getTotalAmount();
        ReceiveDTO request = receiveRequest(total);

        settleService.receive(settleUuid, request);
        settleService.receive(settleUuid, request);

        Long count = receiveRecordMapper.selectCount(new LambdaQueryWrapper<ReceiveRecord>()
                .eq(ReceiveRecord::getSettleUuid, settleUuid));
        assertThat(count).isEqualTo(1);
        assertThat(settleService.getById(settleUuid).getReceivedAmount()).isEqualByComparingTo(total);
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        SettleByOrderDTO request = new SettleByOrderDTO();
        request.setOrderUuid(orderUuid);
        request.setSettleDate(LocalDate.now());
        request.setIsInvoice(2);
        return request;
    }

    private ReceiveDTO receiveRequest(BigDecimal amount) {
        ReceiveDTO request = new ReceiveDTO();
        request.setRequestId("idempotency-request-1");
        request.setCashAmount(amount);
        request.setPayMethod(2);
        return request;
    }
}
