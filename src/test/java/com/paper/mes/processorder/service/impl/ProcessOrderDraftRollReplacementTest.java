package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.DraftOrderReader;
import com.paper.mes.processorder.service.DraftOrderVersionGuard;
import com.paper.mes.processorder.service.DraftRollProcessManager;
import com.paper.mes.processorder.service.OriginalRollImportParser;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessPlanDraftManager;
import com.paper.mes.processorder.service.ProcessPlanMapper;
import com.paper.mes.processorder.service.ProcessRouteDraftManager;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessOrderDraftRollReplacementTest {

    @Mock private ProcessOrderMapper processOrderMapper;
    @Mock private OriginalRollMapper originalRollMapper;
    @Mock private ProcessConfigDraftMapper draftMapper;
    @Mock private ProcessStepMapper processStepMapper;
    @Mock private FinishRollMapper finishRollMapper;
    @Mock private CustomerService customerService;
    @Mock private ProcessOrderService processOrderService;
    @Mock private DraftOrderReader draftOrderReader;
    @Mock private ProcessPlanDraftManager planDraftManager;
    @Mock private ProcessRouteDraftManager routeDraftManager;
    @Mock private ProcessPlanMapper processPlanMapper;
    @Mock private OriginalRollImportParser importParser;
    @Mock private ObjectMapper objectMapper;
    @Mock private DocumentNoService documentNoService;
    @Mock private BusinessLockService businessLockService;
    @Mock private DraftOrderVersionGuard versionGuard;
    @Mock private DraftRollProcessManager rollProcessManager;
    @InjectMocks private ProcessOrderDraftServiceImpl service;

    @Test
    void replaceOriginalRolls_removesOldStepsBeforeReplacingRolls() {
        when(processOrderMapper.selectById("order-1")).thenReturn(draftOrder());

        service.replaceOriginalRolls("order-1", List.of(roll()), 7);

        verify(versionGuard).assertExpected(any(ProcessOrder.class), org.mockito.ArgumentMatchers.eq(7));
        InOrder deletionOrder = inOrder(processStepMapper, draftMapper, originalRollMapper);
        deletionOrder.verify(processStepMapper).delete(any());
        deletionOrder.verify(draftMapper).delete(any());
        deletionOrder.verify(originalRollMapper).delete(any());
        verify(originalRollMapper).insert(any(OriginalRoll.class));
    }

    private ProcessOrder draftOrder() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderNo("PO-1");
        order.setOrderStatus(0);
        order.setVersion(7);
        return order;
    }

    private OriginalRollDTO roll() {
        OriginalRollDTO roll = new OriginalRollDTO();
        roll.setPaperName("test paper");
        roll.setRollWeight(BigDecimal.TEN);
        roll.setPieceNum(1);
        roll.setProcessMode(1);
        roll.setMainStepType(2);
        return roll;
    }

}
