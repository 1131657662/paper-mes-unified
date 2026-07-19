package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void receive_whenRequestIdReusesDifferentPayload_rejectsReplay() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        String settleUuid = settleService.createByOrder(settleRequest(scenario.orderUuid()));
        BigDecimal total = settleService.getById(settleUuid).getTotalAmount();
        ReceiveDTO first = receiveRequest(total);
        ReceiveDTO changed = receiveRequest(total.subtract(new BigDecimal("1.00")));

        settleService.receive(settleUuid, first);

        assertThatThrownBy(() -> settleService.receive(settleUuid, changed))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求号已用于其他收款内容");
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        return SettlementTestRequestFactory.byOrder(settleService, orderUuid);
    }

    private ReceiveDTO receiveRequest(BigDecimal amount) {
        ReceiveDTO request = new ReceiveDTO();
        request.setRequestId("idempotency-request-1");
        request.setCashAmount(amount);
        request.setPayMethod(2);
        request.setPayNo("TX-IDEMPOTENT-1");
        return request;
    }
}
