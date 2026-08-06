package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaEngine;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.*;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DeliveryCustomerRevisionPreviewServiceTest {

    @Test
    void current_with128ValidItems_doesNotReportErrors() {
        DeliveryOrderMapper orderMapper = mock(DeliveryOrderMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryService deliveryService = mock(DeliveryService.class);
        DeliveryCustomerRevisionReader reader = mock(DeliveryCustomerRevisionReader.class);
        DeliveryCustomerSpecPlanner planner = mock(DeliveryCustomerSpecPlanner.class);
        DeliveryOrder order = completedOrder();
        List<DeliveryDetailItemVO> physicalItems = IntStream.range(0, 128).mapToObj(this::physicalItem).toList();
        DeliveryDetailVO delivery = deliveryDetail(order);
        delivery.setDetails(physicalItems);
        when(orderMapper.selectById("delivery-1")).thenReturn(order);
        when(deliveryService.getDetail("delivery-1")).thenReturn(delivery);
        when(detailMapper.selectBatchIds(anyList())).thenReturn(IntStream.range(0, 128).mapToObj(this::detail).toList());
        when(finishMapper.selectBatchIds(anyList())).thenReturn(IntStream.range(0, 128).mapToObj(this::finish).toList());
        when(reader.latestItems(eq("delivery-1"), anyList())).thenReturn(Map.of());
        when(reader.nextRevisionNo("delivery-1")).thenReturn(1);
        when(planner.current(any())).thenAnswer(ignored -> validCustomerSpec());

        var result = new DeliveryCustomerRevisionPreviewService(
                orderMapper, detailMapper, finishMapper, deliveryService, reader, planner).current("delivery-1");

        assertThat(result.getItemCount()).isEqualTo(128);
        assertThat(result.getValidItemCount()).isEqualTo(128);
        assertThat(result.isHasErrors()).isFalse();
    }

    @Test
    void current_voidedDeliveryRemainsReadableButPreviewRejectsEdit() {
        DeliveryOrderMapper orderMapper = mock(DeliveryOrderMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryService deliveryService = mock(DeliveryService.class);
        DeliveryCustomerRevisionReader reader = mock(DeliveryCustomerRevisionReader.class);
        DeliveryOrder order = new DeliveryOrder();
        order.setUuid("delivery-1");
        order.setDeliveryStatus(3);
        order.setVersion(1);
        when(orderMapper.selectById("delivery-1")).thenReturn(order);
        when(deliveryService.getDetail("delivery-1")).thenReturn(deliveryDetail(order));
        when(detailMapper.selectBatchIds(List.of("detail-1"))).thenReturn(List.of(detail()));
        when(finishMapper.selectBatchIds(List.of("finish-1"))).thenReturn(List.of(changedFinish()));
        when(reader.latestItems("delivery-1", List.of("detail-1"))).thenReturn(Map.of());
        when(reader.nextRevisionNo("delivery-1")).thenReturn(1);
        DeliveryCustomerRevisionPreviewService service = new DeliveryCustomerRevisionPreviewService(
                orderMapper, detailMapper, finishMapper, deliveryService, reader,
                new DeliveryCustomerSpecPlanner(new CustomerWeightFormulaEngine()));

        var current = service.current("delivery-1");

        assertThat(current.getItemCount()).isEqualTo(1);
        assertThat(current.getCurrentRevisionKind()).isEqualTo("HISTORICAL_BASELINE");
        assertThatThrownBy(() -> service.preview("delivery-1", new com.paper.mes.delivery.dto.DeliveryCustomerRevisionRequestDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已作废出库单");
    }

    @Test
    void current_completedLegacyDeliveryWithoutRevision_usesPhysicalSnapshotBaseline() {
        DeliveryOrderMapper orderMapper = mock(DeliveryOrderMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryService deliveryService = mock(DeliveryService.class);
        DeliveryCustomerRevisionReader reader = mock(DeliveryCustomerRevisionReader.class);
        DeliveryOrder order = completedOrder();
        DeliveryDetail detail = detail();
        FinishRoll finish = changedFinish();
        when(orderMapper.selectById("delivery-1")).thenReturn(order);
        when(deliveryService.getDetail("delivery-1")).thenReturn(deliveryDetail(order));
        when(detailMapper.selectBatchIds(List.of("detail-1"))).thenReturn(List.of(detail));
        when(finishMapper.selectBatchIds(List.of("finish-1"))).thenReturn(List.of(finish));
        when(reader.latestItems("delivery-1", List.of("detail-1"))).thenReturn(java.util.Map.of());
        when(reader.nextRevisionNo("delivery-1")).thenReturn(1);
        DeliveryCustomerRevisionPreviewService service = new DeliveryCustomerRevisionPreviewService(
                orderMapper, detailMapper, finishMapper, deliveryService, reader,
                new DeliveryCustomerSpecPlanner(new CustomerWeightFormulaEngine()));

        var result = service.current("delivery-1");

        assertThat(result.getCurrentRevisionKind()).isEqualTo("HISTORICAL_BASELINE");
        assertThat(result.getItems().getFirst().getCustomerPaperName()).isEqualTo("白卡");
        assertThat(result.getItems().getFirst().getCustomerGramWeight()).isEqualTo(70);
        assertThat(result.getCustomerTotalWeight()).isEqualByComparingTo("500.000");
    }

    private DeliveryOrder completedOrder() {
        DeliveryOrder order = new DeliveryOrder();
        order.setUuid("delivery-1");
        order.setDeliveryNo("CK-001");
        order.setDeliveryStatus(2);
        order.setVersion(1);
        return order;
    }

    private DeliveryDetail detail() {
        DeliveryDetail detail = new DeliveryDetail();
        detail.setUuid("detail-1");
        detail.setFinishUuid("finish-1");
        detail.setVersion(1);
        return detail;
    }

    private DeliveryDetailItemVO physicalItem(int index) {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setUuid("detail-" + index);
        item.setFinishUuid("finish-" + index);
        return item;
    }

    private DeliveryDetail detail(int index) {
        DeliveryDetail detail = new DeliveryDetail();
        detail.setUuid("detail-" + index);
        detail.setFinishUuid("finish-" + index);
        return detail;
    }

    private FinishRoll finish(int index) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-" + index);
        return finish;
    }

    private DeliveryCustomerSpecVO validCustomerSpec() {
        DeliveryCustomerSpecVO item = new DeliveryCustomerSpecVO();
        item.setValid(true);
        item.setPhysicalDeliveryWeight(BigDecimal.ONE);
        item.setCustomerDisplayWeight(BigDecimal.ONE);
        return item;
    }

    private DeliveryDetailVO deliveryDetail(DeliveryOrder order) {
        DeliveryDetailItemVO item = new DeliveryDetailItemVO();
        item.setUuid("detail-1");
        item.setFinishUuid("finish-1");
        item.setPaperName("白卡");
        item.setGramWeight(70);
        item.setFinishWidth(1000);
        item.setOutWeight(new BigDecimal("500"));
        DeliveryDetailVO result = new DeliveryDetailVO();
        result.setOrder(order);
        result.setDetails(List.of(item));
        return result;
    }

    private FinishRoll changedFinish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setCustomerPaperName("食品卡");
        finish.setCustomerGramWeight(75);
        finish.setCustomerFinishWidth(900);
        finish.setCustomerDisplayWeight(new BigDecimal("550"));
        finish.setActualWeight(new BigDecimal("500"));
        return finish;
    }
}
