package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class FinishCustomerRevisionPreviewServiceTest {

    @Test
    void current_with128ValidItems_doesNotReportErrors() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        FinishCustomerRevisionMapper revisionMapper = mock(FinishCustomerRevisionMapper.class);
        FinishCustomerSpecPlanner planner = mock(FinishCustomerSpecPlanner.class);
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(5);
        List<FinishRoll> finishes = IntStream.range(0, 128).mapToObj(this::finish).toList();
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(finishMapper.selectList(any())).thenReturn(finishes);
        when(planner.current(any())).thenAnswer(ignored -> validCustomerSpec());

        var result = new FinishCustomerRevisionPreviewService(
                orderMapper, finishMapper, revisionMapper, planner,
                new FinishCustomerRevisionPolicy(), mock(ProcessOrderDeliveryImpactCounter.class)).current("order-1");

        assertThat(result.getItemCount()).isEqualTo(128);
        assertThat(result.getValidItemCount()).isEqualTo(128);
        assertThat(result.isHasErrors()).isFalse();
    }

    @Test
    void currentRejectsVoidedOrderUsingCanonicalStatusCode() {
        ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(6);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        FinishCustomerRevisionPreviewService service = new FinishCustomerRevisionPreviewService(
                orderMapper,
                mock(FinishRollMapper.class),
                mock(FinishCustomerRevisionMapper.class),
                mock(FinishCustomerSpecPlanner.class),
                new FinishCustomerRevisionPolicy(), mock(ProcessOrderDeliveryImpactCounter.class));

        assertThatThrownBy(() -> service.current("order-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已作废加工单");
    }

    @Test
    void processingOrder_withDisplayWeightOnlyChange_doesNotRequireReissue() {
        FinishCustomerRevisionPolicy policy = new FinishCustomerRevisionPolicy();
        ProcessOrder order = new ProcessOrder();
        order.setOrderStatus(2);
        FinishCustomerSpecVO row = validCustomerSpec();
        row.setPreviousCustomerPaperName("customer-paper");
        row.setCustomerPaperName("customer-paper");
        row.setPreviousCustomerGramWeight(80);
        row.setCustomerGramWeight(80);
        row.setPreviousCustomerFinishWidth(500);
        row.setCustomerFinishWidth(500);
        row.setPreviousCustomerDisplayWeight(new BigDecimal("100"));
        row.setCustomerDisplayWeight(new BigDecimal("105"));

        assertThat(policy.requiresReissue(order, List.of(row))).isFalse();
    }

    @Test
    void processingOrder_withPrintedSpecificationChange_requiresReissue() {
        FinishCustomerRevisionPolicy policy = new FinishCustomerRevisionPolicy();
        ProcessOrder order = new ProcessOrder();
        order.setOrderStatus(2);
        FinishCustomerSpecVO row = validCustomerSpec();
        row.setPreviousCustomerPaperName("customer-paper");
        row.setCustomerPaperName("new-customer-paper");

        assertThat(policy.requiresReissue(order, List.of(row))).isTrue();
    }

    @Test
    void completedOrder_withPrintedSpecificationChange_rejectsPublishing() {
        FinishCustomerRevisionPolicy policy = new FinishCustomerRevisionPolicy();
        ProcessOrder order = new ProcessOrder();
        order.setOrderStatus(4);
        FinishCustomerSpecVO row = validCustomerSpec();
        row.setPreviousCustomerFinishWidth(500);
        row.setCustomerFinishWidth(520);

        assertThatThrownBy(() -> policy.requirePublishAllowed(order, List.of(row)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能修改已下发的客户品名、克重或门幅");
    }

    @Test
    void settledOrder_withDisplayWeightOnlyChange_allowsPublishing() {
        FinishCustomerRevisionPolicy policy = new FinishCustomerRevisionPolicy();
        ProcessOrder order = new ProcessOrder();
        order.setOrderStatus(5);
        FinishCustomerSpecVO row = validCustomerSpec();
        row.setPreviousCustomerDisplayWeight(new BigDecimal("100"));
        row.setCustomerDisplayWeight(new BigDecimal("105"));

        policy.requirePublishAllowed(order, List.of(row));
    }

    private FinishRoll finish(int index) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-" + index);
        return finish;
    }

    private FinishCustomerSpecVO validCustomerSpec() {
        FinishCustomerSpecVO item = new FinishCustomerSpecVO();
        item.setValid(true);
        item.setPhysicalWeight(BigDecimal.ONE);
        item.setCustomerDisplayWeight(BigDecimal.ONE);
        return item;
    }
}
