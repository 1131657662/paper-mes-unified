package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.mapper.SettleDetailMapper;
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
class DeliverySettleRollbackBusinessFlowIT {

    @Autowired
    private BusinessFlowFixtureFactory fixtures;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private SettleService settleService;
    @Autowired
    private FinishRollMapper finishRollMapper;
    @Autowired
    private ProcessOrderMapper processOrderMapper;
    @Autowired
    private DeliveryDetailMapper deliveryDetailMapper;
    @Autowired
    private SettleDetailMapper settleDetailMapper;

    @Test
    void completedOrder_whenDeliveredRolledBackAndSettlementVoided_restoresBusinessState() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));

        deliveryService.confirm(deliveryUuid, confirmRequest());

        assertThat(deliveryService.getById(deliveryUuid).getDeliveryStatus()).isEqualTo(2);
        assertFinishStock(scenario.first().getUuid(), 3, "0.000");
        assertThat(onlyDeliveryDetail(deliveryUuid).getStockLockStatus()).isZero();

        deliveryService.rollback(deliveryUuid, rollbackRequest());

        assertThat(deliveryService.getById(deliveryUuid).getDeliveryStatus()).isEqualTo(1);
        assertFinishStock(scenario.first().getUuid(), 2, "100.000");
        assertThat(onlyDeliveryDetail(deliveryUuid).getStockLockStatus()).isEqualTo(1);

        String settleUuid = settleService.createByOrder(settleRequest(scenario.order().getUuid()));

        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(5);
        assertThat(settleService.getById(settleUuid).getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(onlySettleDetail(settleUuid).getOrderAmount()).isEqualByComparingTo("100.00");

        settleService.voidSettle(settleUuid, voidRequest());

        assertThat(settleService.getById(settleUuid).getSettleStatus()).isEqualTo(4);
        assertThat(settleService.getById(settleUuid).getVoidReason()).isEqualTo("integration test void");
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(4);
        assertThat(settleDetailMapper.selectCount(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getSettleUuid, settleUuid))).isZero();
    }

    private DeliveryCreateDTO createRequest(BusinessFlowFixtureFactory.Scenario scenario) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(scenario.first().getUuid());
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryConfirmDTO confirmRequest() {
        DeliveryConfirmDTO request = new DeliveryConfirmDTO();
        request.setSignUser("integration-test");
        return request;
    }

    private DeliveryRollbackDTO rollbackRequest() {
        DeliveryRollbackDTO request = new DeliveryRollbackDTO();
        request.setReason("integration test rollback");
        return request;
    }

    private SettleByOrderDTO settleRequest(String orderUuid) {
        return SettlementTestRequestFactory.byOrder(settleService, orderUuid);
    }

    private SettleActionReasonDTO voidRequest() {
        SettleActionReasonDTO request = new SettleActionReasonDTO();
        request.setReason("integration test void");
        return request;
    }

    private DeliveryDetail onlyDeliveryDetail(String deliveryUuid) {
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(
                new LambdaQueryWrapper<DeliveryDetail>().eq(DeliveryDetail::getDeliveryUuid, deliveryUuid));
        assertThat(details).hasSize(1);
        return details.get(0);
    }

    private SettleDetail onlySettleDetail(String settleUuid) {
        List<SettleDetail> details = settleDetailMapper.selectList(
                new LambdaQueryWrapper<SettleDetail>().eq(SettleDetail::getSettleUuid, settleUuid));
        assertThat(details).hasSize(1);
        return details.get(0);
    }

    private void assertFinishStock(String finishUuid, int status, String remainingWeight) {
        FinishRoll finish = finishRollMapper.selectById(finishUuid);
        assertThat(finish.getFinishStatus()).isEqualTo(status);
        assertThat(finish.getRemainingWeight()).isEqualByComparingTo(remainingWeight);
    }
}
