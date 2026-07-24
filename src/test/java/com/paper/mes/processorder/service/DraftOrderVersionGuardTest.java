package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DraftOrderVersionGuardTest {

    private ProcessOrderMapper orderMapper;
    private DraftOrderVersionGuard guard;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProcessOrder.class);
    }

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        guard = new DraftOrderVersionGuard(orderMapper);
    }

    @Test
    void matchingVersion_advancesExactlyOnce() {
        ProcessOrder order = order(4);
        when(orderMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        guard.assertExpected(order, 4);
        guard.advance("order-1", 4);

        verify(orderMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void staleVersion_rejectsBeforeUpdate() {
        ProcessOrder order = order(5);

        assertThatThrownBy(() -> guard.assertExpected(order, 4))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("刷新后重试")
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E006.getCode());

        verifyNoInteractions(orderMapper);
    }

    @Test
    void zeroUpdatedRows_isConcurrencyConflict() {
        when(orderMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> guard.advance("order-1", 4))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E006.getCode());
    }

    private ProcessOrder order(int version) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setVersion(version);
        return order;
    }
}
