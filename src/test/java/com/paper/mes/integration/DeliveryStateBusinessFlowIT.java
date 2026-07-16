package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryCancelDTO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DeliveryStateBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryDetailMapper deliveryDetailMapper;
    @Autowired private FinishRollMapper finishRollMapper;
    @Autowired private ProcessOrderMapper processOrderMapper;

    @Test
    void pendingDelivery_whenRollbackBeforeConfirm_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));

        assertThatThrownBy(() -> deliveryService.rollback(deliveryUuid, rollbackRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非已出库状态");
    }

    @Test
    void confirmedDelivery_whenConfirmRepeated_rejectsWithoutDoubleDeductingStock() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());

        assertThatThrownBy(() -> deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非待出库状态");
        assertFinishStock(scenario.first().getUuid(), 3, "0.000");
    }

    @Test
    void rolledBackDelivery_whenRollbackRepeated_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());
        deliveryService.rollback(deliveryUuid, rollbackRequest());

        assertThatThrownBy(() -> deliveryService.rollback(deliveryUuid, rollbackRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非已出库状态");
    }

    @Test
    void confirmedDelivery_whenAppendRequested_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());

        assertThatThrownBy(() -> deliveryService.appendDetails(
                deliveryUuid, appendRequest(scenario.second().getUuid())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待出库单允许追加明细");
    }

    @Test
    void confirmedDelivery_whenRemoveRequested_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        String detailUuid = onlyDetail(deliveryUuid).getUuid();
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());

        assertThatThrownBy(() -> deliveryService.removeDetail(deliveryUuid, detailUuid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待出库单允许移出明细");
    }

    @Test
    void confirmedDelivery_whenCancelRequested_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());

        assertThatThrownBy(() -> deliveryService.cancelPending(deliveryUuid, cancelRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待出库单允许作废");
    }

    @Test
    void pendingDelivery_whenAppendingFinishFromNonDeliverableOrder_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        scenario.order().setOrderStatus(3);
        processOrderMapper.updateById(scenario.order());

        assertThatThrownBy(() -> deliveryService.appendDetails(
                deliveryUuid, appendRequest(scenario.second().getUuid())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("加工单非可出库状态");
        assertThat(deliveryDetails(deliveryUuid)).hasSize(1);
    }

    @Test
    void cancelledDelivery_whenCancelledRepeated_rejectsOperation() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        String deliveryUuid = deliveryService.create(createRequest(scenario));
        deliveryService.cancelPending(deliveryUuid, cancelRequest());

        assertThatThrownBy(() -> deliveryService.cancelPending(deliveryUuid, cancelRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待出库单允许作废");
        assertThat(deliveryService.getById(deliveryUuid).getDeliveryStatus()).isEqualTo(3);
        assertThat(deliveryService.getById(deliveryUuid).getVoidReason()).isEqualTo("state test cancellation");
    }

    private DeliveryCreateDTO createRequest(BusinessFlowFixtureFactory.Scenario scenario) {
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(createItem(scenario.first().getUuid())));
        return request;
    }

    private DeliveryCreateDTO.Item createItem(String finishUuid) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(finishUuid);
        item.setOutWeight(new BigDecimal("100.000"));
        return item;
    }

    private DeliveryAppendItemsDTO appendRequest(String finishUuid) {
        DeliveryAppendItemsDTO request = new DeliveryAppendItemsDTO();
        DeliveryAppendItemsDTO.Item item = new DeliveryAppendItemsDTO.Item();
        item.setFinishUuid(finishUuid);
        item.setOutWeight(new BigDecimal("100.000"));
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryRollbackDTO rollbackRequest() {
        DeliveryRollbackDTO request = new DeliveryRollbackDTO();
        request.setReason("state test rollback");
        return request;
    }

    private DeliveryCancelDTO cancelRequest() {
        DeliveryCancelDTO request = new DeliveryCancelDTO();
        request.setReason("state test cancellation");
        return request;
    }

    private DeliveryDetail onlyDetail(String deliveryUuid) {
        List<DeliveryDetail> details = deliveryDetails(deliveryUuid);
        assertThat(details).hasSize(1);
        return details.getFirst();
    }

    private List<DeliveryDetail> deliveryDetails(String deliveryUuid) {
        return deliveryDetailMapper.selectList(new LambdaQueryWrapper<DeliveryDetail>()
                .eq(DeliveryDetail::getDeliveryUuid, deliveryUuid));
    }

    private void assertFinishStock(String finishUuid, int status, String remainingWeight) {
        FinishRoll finish = finishRollMapper.selectById(finishUuid);
        assertThat(finish.getFinishStatus()).isEqualTo(status);
        assertThat(finish.getRemainingWeight()).isEqualByComparingTo(remainingWeight);
    }
}
