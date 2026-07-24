package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.customerdisplay.formula.*;
import com.paper.mes.delivery.dto.*;
import com.paper.mes.delivery.entity.*;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionMapper;
import com.paper.mes.delivery.service.*;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DeliveryCustomerRevisionBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryCustomerRevisionPreviewService previewService;
    @Autowired private DeliveryCustomerRevisionPublisher publisher;
    @Autowired private DeliveryCustomerRevisionReader reader;
    @Autowired private DeliveryCustomerRevisionMapper revisionMapper;
    @Autowired private FinishRollMapper finishMapper;
    @Autowired private WarehouseMapper warehouseMapper;

    @Test
    void completedDelivery_customerRevisionLeavesSnapshotInventoryAndPhysicalTotalsUntouched() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        Warehouse warehouse = createWarehouse();
        applyCustomerDefaults(scenario.first(), warehouse.getUuid());
        String deliveryUuid = deliveryService.create(createRequest(scenario, warehouse.getUuid()));
        deliveryService.confirm(deliveryUuid, new DeliveryConfirmDTO());
        DeliveryOrder signed = deliveryService.getById(deliveryUuid);
        DeliveryBaseline baseline = new DeliveryBaseline(signed.getSnapDelivery(), signed.getTotalWeight());

        DeliveryCustomerRevisionPreviewVO current = previewService.current(deliveryUuid);
        DeliveryCustomerRevisionRequestDTO request = revisionRequest(current);
        DeliveryCustomerRevisionSummaryVO published = publisher.publish(deliveryUuid, request);
        DeliveryCustomerRevisionSummaryVO replay = publisher.publish(deliveryUuid, request);

        DeliveryOrder after = deliveryService.getById(deliveryUuid);
        FinishRoll finishAfter = finishMapper.selectById(scenario.first().getUuid());
        assertPublishedOnce(deliveryUuid, published, replay);
        assertPhysicalFactsUntouched(after, finishAfter, baseline);
        assertRevisionDetail(deliveryUuid, published.getUuid(), scenario.first().getFinishRollNo());
        assertThat(previewService.current(deliveryUuid).getItems().getFirst().getValueSource())
                .isEqualTo("DELIVERY_REVISION");
    }

    private void assertPublishedOnce(String deliveryUuid, DeliveryCustomerRevisionSummaryVO published,
                                     DeliveryCustomerRevisionSummaryVO replay) {
        assertThat(published.getRevisionNo()).isEqualTo(1);
        assertThat(replay.getUuid()).isEqualTo(published.getUuid());
        assertThat(revisionMapper.selectCount(new LambdaQueryWrapper<DeliveryCustomerRevision>()
                .eq(DeliveryCustomerRevision::getDeliveryUuid, deliveryUuid))).isEqualTo(1);
    }

    private void assertPhysicalFactsUntouched(DeliveryOrder after, FinishRoll finishAfter,
                                              DeliveryBaseline baseline) {
        assertThat(after.getSnapDelivery()).isEqualTo(baseline.snapshot());
        assertThat(after.getTotalWeight()).isEqualByComparingTo(baseline.totalWeight());
        assertThat(after.getDeliveryStatus()).isEqualTo(2);
        assertThat(finishAfter.getFinishStatus()).isEqualTo(3);
    }

    private void assertRevisionDetail(String deliveryUuid, String revisionUuid, String finishRollNo) {
        DeliveryCustomerRevisionDetailVO detail = reader.detail(deliveryUuid, revisionUuid);
        assertThat(detail.getItems()).hasSize(1);
        assertThat(detail.getItems().getFirst().getFinishRollNo()).isEqualTo(finishRollNo);
        assertThat(detail.getItems().getFirst().getCustomerPaperName()).isEqualTo("食品卡");
        assertThat(detail.getItems().getFirst().getWeightOperand()).isEqualByComparingTo("5");
    }

    private Warehouse createWarehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid(java.util.UUID.randomUUID().toString().replace("-", ""));
        warehouse.setWarehouseCode("IT-WH-" + warehouse.getUuid().substring(0, 8));
        warehouse.setWarehouseName("集成测试成品仓");
        warehouse.setStatus(1);
        warehouse.setIsDefault(0);
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    private void applyCustomerDefaults(FinishRoll finish, String warehouseUuid) {
        finish.setCustomerPaperName("食品卡");
        finish.setCustomerGramWeight(75);
        finish.setCustomerFinishWidth(900);
        finish.setCustomerDisplayWeight(finish.getActualWeight().multiply(new BigDecimal("1.1")));
        finish.setWarehouseUuid(warehouseUuid);
        finishMapper.updateById(finish);
    }

    private DeliveryCreateDTO createRequest(BusinessFlowFixtureFactory.Scenario scenario, String warehouseUuid) {
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid(scenario.first().getUuid());
        item.setOutWeight(new BigDecimal("100.000"));
        DeliveryCreateDTO request = new DeliveryCreateDTO();
        request.setCustomerUuid(scenario.customer().getUuid());
        request.setWarehouseUuid(warehouseUuid);
        request.setDeliveryDate(LocalDate.now());
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryCustomerRevisionRequestDTO revisionRequest(
            DeliveryCustomerRevisionPreviewVO current) {
        DeliveryCustomerSpecVO row = current.getItems().getFirst();
        DeliveryCustomerSpecItemDTO item = new DeliveryCustomerSpecItemDTO();
        item.setDeliveryDetailUuid(row.getDeliveryDetailUuid());
        item.setExpectedDetailVersion(row.getDetailVersion());
        item.setCustomerPaperName(row.getCustomerPaperName());
        item.setCustomerGramWeight(row.getCustomerGramWeight());
        item.setCustomerFinishWidth(row.getCustomerFinishWidth());
        item.setCalculationMode(CustomerWeightCalculationMode.DELTA);
        item.setWeightOperand(new BigDecimal("5"));
        item.setRoundingScale(3);
        item.setRoundingMode(RoundingMode.HALF_UP);
        item.setZeroPolicy(CustomerWeightZeroPolicy.SKIP);
        DeliveryCustomerRevisionRequestDTO request = new DeliveryCustomerRevisionRequestDTO();
        request.setRequestId("delivery-revision-it-1");
        request.setExpectedDeliveryVersion(current.getDeliveryVersion());
        request.setReason("客户完结后要求更正单据");
        request.setItems(List.of(item));
        return request;
    }

    private record DeliveryBaseline(String snapshot, BigDecimal totalWeight) {
    }
}
