package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.exporttask.dto.ExportTaskCreateDTO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentExportTaskReplayTest {
    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void createProcessOrderTask_whenRequestBelongsToAnotherOrder_rejectsReplay() {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        ExportTaskDocumentResolver resolver = mock(ExportTaskDocumentResolver.class);
        ProcessOrderExportRevisionSnapshot revisionSnapshot = mock(ProcessOrderExportRevisionSnapshot.class);
        ExportTaskCreationService service = service(taskMapper, resolver, revisionSnapshot);
        when(taskMapper.selectOne(any())).thenReturn(existingTask());
        when(resolver.processOrder("order-1")).thenReturn(new ProcessOrder());
        when(revisionSnapshot.capture("order-1", null)).thenReturn("{}");
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());

        assertThatThrownBy(() -> service.createProcessOrderTask("order-1", request()))
                .isInstanceOf(BusinessException.class);
    }

    private ExportTaskCreationService service(
            ExportTaskMapper mapper, ExportTaskDocumentResolver resolver,
            ProcessOrderExportRevisionSnapshot revisionSnapshot) {
        return new ExportTaskCreationService(mapper, mock(ExportTaskExecutor.class),
                mock(ExportTaskLifecycleService.class), resolver, mock(PermissionChecker.class),
                new ExportTaskPayloadWriter(new com.fasterxml.jackson.databind.ObjectMapper()),
                mock(ExportTaskStorage.class), mock(com.paper.mes.report.service.ReportQuerySnapshotService.class),
                mock(DeliveryOrderExportRevisionSnapshot.class), revisionSnapshot,
                mock(DeliveryExportSnapshotTaskCreator.class));
    }

    private ExportTask existingTask() {
        ExportTask task = new ExportTask();
        task.setTaskType(ProcessOrderDetailExportTaskHandler.TASK_TYPE);
        task.setSourceUuid("order-2");
        task.setRequestPayload("{}");
        return task;
    }

    private ExportTaskCreateDTO request() {
        ExportTaskCreateDTO dto = new ExportTaskCreateDTO();
        dto.setRequestId("same-request");
        return dto;
    }
}
