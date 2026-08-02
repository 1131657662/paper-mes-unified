package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.DamageImageService;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessOrderFinishConfigVersionTest {

    private final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    private final FinishRollMapper finishMapper = mock(FinishRollMapper.class);
    private final ProcessStepMapper stepMapper = mock(ProcessStepMapper.class);
    private final DocumentNoService documentNoService = mock(DocumentNoService.class);
    private VersionGuardedService service;

    @BeforeEach
    void setUp() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setVersion(4);
        service = new VersionGuardedService(order);
    }

    @Test
    void saveFinishConfig_withStaleVersion_rejectsBeforeWrites() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveFinishConfig(
                        "order-1", "roll-1", new FinishConfigSaveDTO(), 3));

        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
        verifyNoPersistenceInteractions();
    }

    @Test
    void saveFinishConfigBatch_withStaleVersion_rejectsBeforeWrites() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveFinishConfigBatch(
                        "order-1", new FinishConfigBatchSaveDTO(), 3));

        assertEquals(ErrorCode.E006.getCode(), error.getErrorCode());
        verifyNoPersistenceInteractions();
    }

    @Test
    void finishConfig_recalculatesOrderAggregateWithoutIntermediateVersionUpdates() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java"),
                StandardCharsets.UTF_8);
        String singleSave = slice(source, "private FinishConfigSaveVO saveFinishConfigInternal",
                "public FinishPreviewVO previewRewindPlan");
        String batchSave = slice(source, "private FinishConfigBatchSaveVO persistFinishConfigBatch",
                "private void validateFinishConfigBatchItems");

        assertFalse(singleSave.contains("updateMixProcessFlag(orderUuid)"));
        assertTrue(batchSave.contains("calcFee(orderUuid)"));
        assertFalse(batchSave.contains("updateMixProcessFlag(orderUuid)"));
    }

    private void verifyNoPersistenceInteractions() {
        verifyNoInteractions(rollMapper, finishMapper, stepMapper, documentNoService);
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "Missing start marker: " + start);
        assertTrue(endIndex >= 0, "Missing end marker: " + end);
        return source.substring(startIndex, endIndex);
    }

    private final class VersionGuardedService extends ProcessOrderServiceImpl {
        private final ProcessOrder order;

        private VersionGuardedService(ProcessOrder order) {
            super(rollMapper, finishMapper, stepMapper, mock(ProcessParamMapper.class),
                    mock(ProcessStageInputRelMapper.class), mock(ProcessStageOutputMapper.class),
                    mock(FinishOriginalRelMapper.class), mock(DeliveryDetailMapper.class),
                    mock(SettleDetailMapper.class), mock(CustomerService.class),
                    mock(OperationLogService.class), new ObjectMapper(), mock(DamageImageService.class),
                    mock(RollNoSequenceService.class), new SawPlanPreviewer(), documentNoService,
                    mock(BusinessLockService.class), mock(MachineMapper.class), mock(WeightCheckThresholdService.class),
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    new com.paper.mes.processorder.service.ProcessOrderSettlementPolicy(),
                    mock(InventoryLedgerBusinessRecorder.class));
            this.order = order;
        }

        @Override
        public ProcessOrder getById(java.io.Serializable id) {
            return order;
        }
    }
}
