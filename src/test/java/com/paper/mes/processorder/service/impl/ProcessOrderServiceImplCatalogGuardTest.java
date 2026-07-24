package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.BackRecordWarehousePolicy;
import com.paper.mes.processorder.service.BackRecordScopeResolver;
import com.paper.mes.processorder.service.DamageImageService;
import com.paper.mes.processorder.service.ProcessCatalogStepValidator;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

class ProcessOrderServiceImplCatalogGuardTest {

    private final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    private final ProcessStepMapper stepMapper = mock(ProcessStepMapper.class);
    private final ProcessCatalogStepValidator catalogValidator = mock(ProcessCatalogStepValidator.class);

    @Test
    void addRoll_whenMainProcessIsDisabled_rejectsBeforeWritingRollOrStep() {
        ProcessOrderServiceImpl service = service(pendingOrder());
        doThrow(new BusinessException("工序类型未启用或不存在"))
                .when(catalogValidator).validateMainProcess(1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.addRoll("order-1", sawRoll()));

        assertEquals("工序类型未启用或不存在", error.getMessage());
        verify(rollMapper, never()).insert(any(OriginalRoll.class));
        verify(stepMapper, never()).insert(any(ProcessStep.class));
    }

    private ProcessOrderServiceImpl service(ProcessOrder order) {
        return new ProcessOrderServiceImpl(
                rollMapper,
                mock(FinishRollMapper.class),
                stepMapper,
                mock(ProcessParamMapper.class),
                mock(ProcessStageInputRelMapper.class),
                mock(ProcessStageOutputMapper.class),
                mock(FinishOriginalRelMapper.class),
                mock(DeliveryDetailMapper.class),
                mock(SettleDetailMapper.class),
                mock(CustomerService.class),
                mock(OperationLogService.class),
                new ObjectMapper(),
                mock(DamageImageService.class),
                mock(RollNoSequenceService.class),
                new SawPlanPreviewer(),
                mock(DocumentNoService.class),
                mock(BusinessLockService.class),
                mock(MachineMapper.class),
                mock(WeightCheckThresholdService.class),
                null,
                null,
                null,
                new BackRecordScopeResolver(),
                null,
                mock(BackRecordWarehousePolicy.class),
                null,
                null,
                null,
                null,
                catalogValidator,
                null,
                null,
                null) {
            @Override
            public ProcessOrder getById(java.io.Serializable id) {
                return order;
            }
        };
    }

    private ProcessOrder pendingOrder() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(1);
        return order;
    }

    private OriginalRollDTO sawRoll() {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        dto.setRollWeight(new BigDecimal("1000.000"));
        dto.setPieceNum(1);
        return dto;
    }
}
