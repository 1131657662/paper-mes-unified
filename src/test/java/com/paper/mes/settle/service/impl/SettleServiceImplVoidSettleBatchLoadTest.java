package com.paper.mes.settle.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import com.paper.mes.settle.service.SettleCandidateStatsLoader;
import com.paper.mes.settle.service.SettleCandidateAmountLoader;
import com.paper.mes.settle.service.SettlementAmountCalculator;
import com.paper.mes.system.config.service.DocumentNoService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettleServiceImplVoidSettleBatchLoadTest {

    @Mock private SettleOrderMapper settleOrderMapper;
    @Mock private SettleDetailMapper settleDetailMapper;
    @Mock private ReceiveRecordMapper receiveRecordMapper;
    @Mock private OriginalRollMapper originalRollMapper;
    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishOriginalRelMapper finishOriginalRelMapper;
    @Mock private ProcessStepMapper processStepMapper;
    @Mock private ProcessStageOutputMapper processStageOutputMapper;
    @Mock private ProcessOrderMapper processOrderMapper;
    @Mock private ProcessOrderService processOrderService;
    @Mock private CustomerService customerService;
    @Mock private MachineMapper machineMapper;
    @Mock private OperationLogMapper operationLogMapper;
    @Mock private OperationLogService operationLogService;
    @Mock private SettleCandidateStatsLoader statsLoader;
    @Mock private SettleCandidateAmountLoader candidateAmountLoader;
    @Mock private SettlementAmountCalculator settlementAmountCalculator;
    @Mock private com.paper.mes.settle.service.SettlementQuoteFactory settlementQuoteFactory;
    @Mock private com.paper.mes.settle.service.SettlementQuoteGuard settlementQuoteGuard;
    @Mock private com.paper.mes.settle.service.SettlementDiscountPolicy settlementDiscountPolicy;
    @Mock private SettlePageDataLoader pageDataLoader;
    @Mock private DocumentNoService documentNoService;
    @Mock private BusinessLockService businessLockService;

    private SettleServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ReceiveRecord.class);
        TableInfoHelper.initTableInfo(assistant, SettleDetail.class);
        TableInfoHelper.initTableInfo(assistant, SettleOrder.class);
        TableInfoHelper.initTableInfo(assistant, ProcessOrder.class);
    }

    @BeforeEach
    void setUp() {
        service = new SettleServiceImpl(settleDetailMapper, receiveRecordMapper, originalRollMapper,
                finishRollMapper, finishOriginalRelMapper, processStepMapper, processStageOutputMapper,
                processOrderService, customerService, machineMapper, operationLogMapper, operationLogService,
                statsLoader, candidateAmountLoader, settlementAmountCalculator, settlementQuoteFactory,
                settlementQuoteGuard, settlementDiscountPolicy, pageDataLoader,
                documentNoService, businessLockService,
                new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseMapper", settleOrderMapper);
    }

    @Test
    void voidSettle_withMultipleDetails_batchLoadsProcessOrders() {
        when(settleOrderMapper.selectById("settle-1")).thenReturn(settle());
        when(receiveRecordMapper.selectList(any())).thenReturn(List.of());
        when(settleDetailMapper.selectList(any())).thenReturn(List.of(
                detail("detail-1", "order-1"),
                detail("detail-2", "order-2")));
        when(processOrderService.listByIds(any())).thenReturn(List.of(
                order("order-1"),
                order("order-2")));
        when(processOrderService.getBaseMapper()).thenReturn(processOrderMapper);
        when(processOrderMapper.update(any(), any())).thenReturn(1);
        when(settleDetailMapper.delete(any())).thenReturn(1);
        when(settleOrderMapper.update(any(), any())).thenReturn(1);

        List<String> orderUuids = service.voidSettle("settle-1", reason());

        assertEquals(List.of("order-1", "order-2"), orderUuids);
        verify(processOrderService).listByIds(argThat(ids -> containsAll(ids, "order-1", "order-2")));
        verify(processOrderService, never()).getById(any());
    }

    @Test
    void page_withoutStatusFilter_excludesVoidedSettlementsByDefault() {
        when(settleOrderMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.page(new SettleQuery());

        verify(settleOrderMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.getSqlSegment().contains("settle_status")
                        && wrapper.getSqlSegment().contains("<>")));
    }

    @Test
    void receiveState_withVoidedSettlement_preservesVoidedStatusAfterAmountNormalization() {
        SettleOrder voided = settle();
        voided.setSettleStatus(4);

        ReflectionTestUtils.invokeMethod(service, "applyReceiveState", voided,
                new BigDecimal("1320.00"), SettleReceiveTotals.zero());

        assertEquals(4, voided.getSettleStatus());
    }

    private SettleOrder settle() {
        SettleOrder settle = new SettleOrder();
        settle.setUuid("settle-1");
        settle.setSettleNo("JS202607070001");
        settle.setSettleStatus(1);
        settle.setIsDeleted(0);
        return settle;
    }

    private SettleDetail detail(String uuid, String orderUuid) {
        SettleDetail detail = new SettleDetail();
        detail.setUuid(uuid);
        detail.setSettleUuid("settle-1");
        detail.setOrderUuid(orderUuid);
        return detail;
    }

    private ProcessOrder order(String uuid) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(uuid);
        order.setOrderStatus(5);
        return order;
    }

    private SettleActionReasonDTO reason() {
        SettleActionReasonDTO dto = new SettleActionReasonDTO();
        dto.setReason("客户取消结算");
        return dto;
    }

    private boolean containsAll(Collection<?> values, String first, String second) {
        return values.size() == 2 && values.contains(first) && values.contains(second);
    }
}
