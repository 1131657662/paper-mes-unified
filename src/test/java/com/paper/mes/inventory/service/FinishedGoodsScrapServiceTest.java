package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.dto.InventoryScrapDTO;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinishedGoodsScrapServiceTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinishRoll.class);
    }

    @Test
    void nullScrapCommandIsRejectedBeforeLockingTheRoll() {
        var locks = mock(com.paper.mes.common.db.BusinessLockService.class);
        FinishedGoodsScrapService service = new FinishedGoodsScrapService(
                mock(FinishRollMapper.class), mock(DeliveryDetailMapper.class), locks,
                mock(InventoryLedgerBusinessRecorder.class), mock(OperationLogService.class));

        assertThatThrownBy(() -> service.scrap("finish-1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scrap command");
    }

    @Test
    void scrapWritesLedgerBeforeMarkingInStockRollScrapped() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper deliveryMapper = mock(DeliveryDetailMapper.class);
        InventoryLedgerBusinessRecorder recorder = mock(InventoryLedgerBusinessRecorder.class);
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setFinishRollNo("F000001");
        finish.setFinishStatus(2);
        finish.setRemainingWeight(new BigDecimal("25.000"));
        when(finishMapper.selectById("finish-1")).thenReturn(finish);
        when(deliveryMapper.countBlockingDeliveryActivity(List.of("finish-1"))).thenReturn(0L);
        when(finishMapper.update(any(), any())).thenReturn(1);
        FinishedGoodsScrapService service = new FinishedGoodsScrapService(
                finishMapper, deliveryMapper, mock(com.paper.mes.common.db.BusinessLockService.class),
                recorder, mock(OperationLogService.class));
        InventoryScrapDTO command = new InventoryScrapDTO();
        command.setReason("破损");
        command.setRequestUuid("scrap-request-1");

        service.scrap("finish-1", command);

        verify(recorder).scrap(eq(finish), eq("scrap-request-1"), eq("破损"),
                eq(new BigDecimal("25.000")), any());
        verify(finishMapper).update(any(), any());
    }
}
