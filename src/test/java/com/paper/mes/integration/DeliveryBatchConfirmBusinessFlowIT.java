package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryBatchConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.customer.mapper.CustomerMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DeliveryBatchConfirmBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryDetailMapper deliveryDetailMapper;
    @Autowired private FinishRollMapper finishRollMapper;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private ProcessStepMapper processStepMapper;
    @Autowired private CustomerMapper customerMapper;
    private final List<BusinessFlowFixtureFactory.Scenario> scenarios = new ArrayList<>();
    private final List<String> deliveryUuids = new ArrayList<>();

    @Test
    void confirmBatch_whenLaterOrderFails_rollsBackEarlierConfirmation() {
        String first = createDelivery(fixtures.createCompletedOrderWithTwoFinishes());
        String second = createDelivery(fixtures.createCompletedOrderWithTwoFinishes());
        List<String> ordered = List.of(first, second).stream().sorted().toList();
        DeliveryDetail validDetail = onlyDetail(ordered.getFirst());
        DeliveryDetail invalidDetail = onlyDetail(ordered.getLast());
        invalidateFinish(invalidDetail.getFinishUuid());
        DeliveryBatchConfirmDTO request = new DeliveryBatchConfirmDTO();
        request.setDeliveryUuids(ordered);

        assertThatThrownBy(() -> deliveryService.confirmBatch(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("成品状态已变更");

        assertThat(deliveryService.getById(ordered.getFirst()).getDeliveryStatus()).isEqualTo(1);
        assertThat(deliveryService.getById(ordered.getLast()).getDeliveryStatus()).isEqualTo(1);
        assertThat(finishRollMapper.selectById(validDetail.getFinishUuid()).getFinishStatus()).isEqualTo(2);
    }

    private String createDelivery(BusinessFlowFixtureFactory.Scenario scenario) {
        scenarios.add(scenario);
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(scenario.first().getUuid());
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(item));
        String deliveryUuid = deliveryService.create(request);
        deliveryUuids.add(deliveryUuid);
        return deliveryUuid;
    }

    private DeliveryDetail onlyDetail(String deliveryUuid) {
        return deliveryDetailMapper.selectList(new LambdaQueryWrapper<DeliveryDetail>()
                .eq(DeliveryDetail::getDeliveryUuid, deliveryUuid)).getFirst();
    }

    private void invalidateFinish(String finishUuid) {
        FinishRoll finish = finishRollMapper.selectById(finishUuid);
        finish.setFinishStatus(3);
        finishRollMapper.updateById(finish);
    }

    @AfterEach
    void cleanCommittedFixtures() {
        for (String deliveryUuid : deliveryUuids) {
            deliveryDetailMapper.delete(new LambdaQueryWrapper<DeliveryDetail>()
                    .eq(DeliveryDetail::getDeliveryUuid, deliveryUuid));
            deliveryService.removeById(deliveryUuid);
        }
        for (BusinessFlowFixtureFactory.Scenario scenario : scenarios) {
            finishRollMapper.deleteById(scenario.first().getUuid());
            finishRollMapper.deleteById(scenario.second().getUuid());
            processStepMapper.delete(new LambdaQueryWrapper<ProcessStep>()
                    .eq(ProcessStep::getOrderUuid, scenario.order().getUuid()));
            processOrderMapper.deleteById(scenario.order().getUuid());
            customerMapper.deleteById(scenario.customer().getUuid());
        }
    }
}
