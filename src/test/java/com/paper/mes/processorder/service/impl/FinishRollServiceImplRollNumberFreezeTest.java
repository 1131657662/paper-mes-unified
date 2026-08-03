package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.FinishRollBatchDTO;
import com.paper.mes.processorder.dto.SpareRollAppendDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.FinishRollSourceBinder;
import com.paper.mes.processorder.service.RollNoSequenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

class FinishRollServiceImplRollNumberFreezeTest {

    private ProcessOrderMapper orderMapper;
    private FinishRollServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        service = newService();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void batchGenerate_rejectsIssuedStatuses(int status) {
        givenOrderStatus(status);

        assertThrows(BusinessException.class,
                () -> service.batchGenerate("order-1", new FinishRollBatchDTO()));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void appendSpare_rejectsIssuedStatuses(int status) {
        givenOrderStatus(status);

        assertThrows(BusinessException.class,
                () -> service.appendSpare("order-1", new SpareRollAppendDTO()));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void voidRollNo_rejectsIssuedStatuses(int status) {
        givenOrderStatus(status);
        FinishRollServiceImpl target = serviceWithFinish();

        assertThrows(BusinessException.class, () -> target.voidRollNo("finish-1"));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void batchVoidRollNo_rejectsIssuedStatuses(int status) {
        givenOrderStatus(status);
        FinishRollServiceImpl target = serviceWithFinish();
        doReturn(List.of(finish())).when(target).listByIds(List.of("finish-1"));

        assertThrows(BusinessException.class, () -> target.batchVoidRollNo(List.of("finish-1")));
    }

    private void givenOrderStatus(int status) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(status);
        when(orderMapper.selectById("order-1")).thenReturn(order);
    }

    private FinishRollServiceImpl serviceWithFinish() {
        FinishRollServiceImpl target = spy(service);
        doReturn(finish()).when(target).getById("finish-1");
        return target;
    }

    private FinishRoll finish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setOrderUuid("order-1");
        finish.setRollNoStatus(1);
        return finish;
    }

    private FinishRollServiceImpl newService() {
        return new FinishRollServiceImpl(
                orderMapper,
                mock(RollNoSequenceService.class),
                mock(FinishRollSourceBinder.class),
                mock(BusinessLockService.class));
    }
}
