package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessStepPricingBatchServiceTest {

    private BusinessLockService lockService;
    private ProcessStepMapper stepMapper;
    private OriginalRollMapper originalRollMapper;
    private SettleDetailMapper settleDetailMapper;
    private ProcessOrderService orderService;
    private ProcessStepPricingApprovalPolicy approvalPolicy;
    private ProcessStepPricingBatchService service;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProcessStep.class);
    }

    @BeforeEach
    void setUp() {
        lockService = mock(BusinessLockService.class);
        stepMapper = mock(ProcessStepMapper.class);
        originalRollMapper = mock(OriginalRollMapper.class);
        settleDetailMapper = mock(SettleDetailMapper.class);
        orderService = mock(ProcessOrderService.class);
        approvalPolicy = mock(ProcessStepPricingApprovalPolicy.class);
        service = new ProcessStepPricingBatchService(lockService, stepMapper, originalRollMapper,
                settleDetailMapper, orderService,
                mock(OperationLogService.class), approvalPolicy);
    }

    @Test
    void preview_withStaleOrderVersion_rejectsBeforeLoadingSteps() {
        when(orderService.getById("order-1")).thenReturn(order(4, 7));

        assertThatThrownBy(() -> service.preview("order-1", request(1, false, "100")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E006.getCode());

        verifyNoInteractions(stepMapper);
    }

    @Test
    void preview_withStepFromAnotherOrder_rejectsSelection() {
        when(orderService.getById("order-1")).thenReturn(order(4, 6));
        when(stepMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.preview("order-1", request(1, false, "100")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E002.getCode());
    }

    @Test
    void preview_withMismatchedStepType_rejectsSelection() {
        when(orderService.getById("order-1")).thenReturn(order(4, 6));
        when(stepMapper.selectList(any())).thenReturn(List.of(step(2)));

        assertThatThrownBy(() -> service.preview("order-1", request(1, false, "100")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E001.getCode());
    }

    @Test
    void apply_toSettledOrder_rejectsAfterLockingOrder() {
        when(orderService.getById("order-1")).thenReturn(order(5, 6));

        assertThatThrownBy(() -> service.apply("order-1", request(1, false, "100")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E001.getCode());

        verify(lockService).lockProcessOrders(List.of("order-1"));
        verifyNoInteractions(stepMapper);
    }

    @Test
    void preview_whenSettlementReferencesOrder_rejectsWithSpecificMessage() {
        when(orderService.getById("order-1")).thenReturn(order(4, 6));
        when(settleDetailMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.preview("order-1", request(1, false, "100")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先作废结算单");

        verifyNoInteractions(stepMapper);
    }

    @Test
    void apply_restoreStandardPrice_usesExplicitNullableColumnUpdate() {
        ProcessOrder order = order(4, 6);
        when(orderService.getById("order-1")).thenReturn(order);
        when(stepMapper.selectList(any())).thenReturn(List.of(step(1)));
        when(stepMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(orderService.calcFee("order-1")).thenReturn(new FeeResultVO());

        service.apply("order-1", request(1, true, null));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Wrapper<ProcessStep>> captor =
                org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(stepMapper).update(isNull(), captor.capture());
        String sqlSet = ((LambdaUpdateWrapper<ProcessStep>) captor.getValue()).getSqlSet();
        assertThat(sqlSet).contains("billing_unit_price").contains("version = version + 1");
        verify(stepMapper, never()).updateById(any(ProcessStep.class));
        verify(approvalPolicy).requireForOrder(any());
    }

    private ProcessOrder order(int status, int version) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderNo("JG-001");
        order.setOrderStatus(status);
        order.setVersion(version);
        return order;
    }

    private ProcessStep step(int type) {
        ProcessStep step = new ProcessStep();
        step.setUuid("step-1");
        step.setOrderUuid("order-1");
        step.setStepType(type);
        step.setStepSort(1);
        step.setVersion(2);
        step.setUnitPrice(new BigDecimal("120"));
        step.setBillingUnitPrice(new BigDecimal("100"));
        step.setStandardQuantity(BigDecimal.ONE);
        step.setBillingQuantity(BigDecimal.ONE);
        return step;
    }

    private ProcessStepPricingBatchDTO request(int type, boolean restore, String price) {
        ProcessStepPricingBatchDTO.Group group = new ProcessStepPricingBatchDTO.Group();
        group.setStepType(type);
        group.setStepUuids(List.of("step-1"));
        group.setRestoreStandard(restore);
        group.setBillingUnitPrice(price == null ? null : new BigDecimal(price));
        ProcessStepPricingBatchDTO dto = new ProcessStepPricingBatchDTO();
        dto.setExpectedOrderVersion(6);
        dto.setReason("customer agreement price");
        dto.setGroups(List.of(group));
        return dto;
    }
}
