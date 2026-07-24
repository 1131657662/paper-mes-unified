package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryExportSnapshotTaskCreatorTest {

    private ExportTaskMapper taskMapper;
    private ExportTaskExecutor executor;
    private DeliveryExportSnapshotCaptureService captureService;
    private DeliveryExportSnapshotTaskCreator creator;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(ExportTaskMapper.class);
        executor = mock(ExportTaskExecutor.class);
        captureService = mock(DeliveryExportSnapshotCaptureService.class);
        ExportTaskStorage storage = mock(ExportTaskStorage.class);
        creator = new DeliveryExportSnapshotTaskCreator(taskMapper, executor, storage, captureService);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createInventory_insertsBeforeCapturingAndSubmitsImmediatelyWithoutTransactionSynchronization() {
        DeliveryInventoryFinishQuery query = new DeliveryInventoryFinishQuery();
        when(taskMapper.insert(any(ExportTask.class))).thenAnswer(invocation -> {
            invocation.<ExportTask>getArgument(0).setUuid("task-1");
            return 1;
        });

        creator.createInventory("request-1", "{}", user(), query);

        var order = inOrder(taskMapper, captureService, executor);
        order.verify(taskMapper).insert(any(ExportTask.class));
        order.verify(captureService).captureInventory(any(ExportTask.class), any());
        order.verify(executor).submit("task-1");
    }

    @Test
    void createInventory_withTransactionSynchronization_submitsOnlyAfterCommit() {
        DeliveryInventoryFinishQuery query = new DeliveryInventoryFinishQuery();
        when(taskMapper.insert(any(ExportTask.class))).thenAnswer(invocation -> {
            invocation.<ExportTask>getArgument(0).setUuid("task-1");
            return 1;
        });
        TransactionSynchronizationManager.initSynchronization();

        creator.createInventory("request-1", "{}", user(), query);
        verify(executor, never()).submit(any());
        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(executor).submit("task-1");
    }

    @Test
    void createInventory_whenRequestIdAlreadyExists_validatesTypeAndPayloadWithoutCapturing() {
        when(taskMapper.insert(any(ExportTask.class))).thenThrow(new DuplicateKeyException("duplicate"));
        ExportTask existing = new ExportTask();
        existing.setUuid("existing-task");
        existing.setTaskType(DeliveryInventoryExportTaskHandler.TASK_TYPE);
        existing.setRequestPayload("different");
        when(taskMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> creator.createInventory("request-1", "{}", user(),
                new DeliveryInventoryFinishQuery()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求号");
        verify(captureService, never()).captureInventory(any(), any());
        verify(executor, never()).submit(any());
    }

    private CurrentUser user() {
        return CurrentUser.builder().uuid("user-1").username("operator").build();
    }
}
