package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverySourceLockServiceTest {

    @Mock private FinishRollMapper finishRollMapper;
    @Mock private ProcessOrderMapper processOrderMapper;
    @Mock private BusinessLockService businessLockService;

    private DeliverySourceLockService service;

    @BeforeEach
    void setUp() {
        service = new DeliverySourceLockService(
                finishRollMapper, processOrderMapper, businessLockService);
    }

    @Test
    void lockAndReload_locksProcessOrderBeforeFinishAndReturnsCurrentRows() {
        FinishRoll initial = finish("finish-1", "order-1");
        FinishRoll current = finish("finish-1", "order-1");
        ProcessOrder order = order("order-1");
        when(finishRollMapper.selectBatchIds(List.of("finish-1"))).thenReturn(List.of(initial));
        when(processOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(finishRollMapper.selectList(any())).thenReturn(List.of(current));

        DeliverySourceLockService.LockedSources result = service.lockAndReload(List.of("finish-1"));

        assertEquals(current, result.finishes().get("finish-1"));
        assertEquals(order, result.processOrders().get("order-1"));
        InOrder ordered = inOrder(finishRollMapper, processOrderMapper, businessLockService);
        ordered.verify(finishRollMapper).selectBatchIds(List.of("finish-1"));
        ordered.verify(businessLockService).lockProcessOrders(List.of("order-1"));
        ordered.verify(processOrderMapper).selectList(any());
        ordered.verify(businessLockService).lockFinishRolls(List.of("finish-1"));
        ordered.verify(finishRollMapper).selectList(any());
    }

    @Test
    void lockAndReload_whenSourceChanges_rejectsStaleRequest() {
        when(finishRollMapper.selectBatchIds(List.of("finish-1")))
                .thenReturn(List.of(finish("finish-1", "order-1")));
        when(processOrderMapper.selectList(any())).thenReturn(List.of(order("order-1")));
        when(finishRollMapper.selectList(any()))
                .thenReturn(List.of(finish("finish-1", "order-2")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.lockAndReload(List.of("finish-1")));

        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
    }

    private FinishRoll finish(String uuid, String orderUuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setOrderUuid(orderUuid);
        return finish;
    }

    private ProcessOrder order(String uuid) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(uuid);
        return order;
    }
}
