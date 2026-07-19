package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.exporttask.dto.DeliveryInventoryExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.DeliveryListExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.ExportTaskCreateDTO;
import com.paper.mes.exporttask.dto.ReportExportTaskCreateDTO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.report.dto.ReportQuery;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class ExportTaskCreationServiceTest {
    private ExportTaskMapper taskMapper;
    private ExportTaskExecutor taskExecutor;
    private ExportTaskDocumentResolver documentResolver;
    private PermissionChecker permissionChecker;
    private ExportTaskStorage storage;
    private ExportTaskCreationService service;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(ExportTaskMapper.class);
        taskExecutor = mock(ExportTaskExecutor.class);
        documentResolver = mock(ExportTaskDocumentResolver.class);
        permissionChecker = mock(PermissionChecker.class);
        storage = mock(ExportTaskStorage.class);
        service = new ExportTaskCreationService(taskMapper, taskExecutor, documentResolver,
                permissionChecker, new ObjectMapper(), storage);
        org.mockito.Mockito.doNothing().when(storage).assertReadyForWrite();
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1")
                .username("operator").realName("操作员").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void createSettleTask_forAuthorizedUser_persistsAndQueuesTask() {
        SettleOrder order = new SettleOrder();
        order.setUuid("settle-1");
        order.setSettleNo("JS-001");
        when(documentResolver.settle("settle-1")).thenReturn(order);
        arrangeInsertedUuid("task-1");
        ExportTaskCreateDTO dto = new ExportTaskCreateDTO();
        dto.setRequestId("request-1");

        String taskUuid = service.createSettleTask("settle-1", dto);

        assertThat(taskUuid).isEqualTo("task-1");
        verify(permissionChecker).require(Permissions.SETTLE_VIEW);
        verify(taskExecutor).submit("task-1");
    }

    @Test
    void createProcessOrderTask_forAuthorizedUser_persistsDocumentTask() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderNo("JG-001");
        when(documentResolver.processOrder("order-1")).thenReturn(order);
        arrangeInsertedUuid("task-order-1");

        String taskUuid = service.createProcessOrderTask("order-1", request("request-order-1"));

        assertDocumentTask(taskUuid, "task-order-1", Permissions.ORDER_VIEW,
                ProcessOrderDetailExportTaskHandler.TASK_TYPE, "process-order", "order-1");
    }

    @Test
    void createDeliveryOrderTask_forAuthorizedUser_persistsDocumentTask() {
        DeliveryOrder order = new DeliveryOrder();
        order.setUuid("delivery-1");
        order.setDeliveryNo("CK-001");
        when(documentResolver.deliveryOrder("delivery-1")).thenReturn(order);
        arrangeInsertedUuid("task-delivery-1");

        String taskUuid = service.createDeliveryOrderTask("delivery-1", request("request-delivery-1"));

        assertDocumentTask(taskUuid, "task-delivery-1", Permissions.DELIVERY_VIEW,
                DeliveryOrderDetailExportTaskHandler.TASK_TYPE, "delivery", "delivery-1");
    }

    @Test
    void createDeliveryInventoryTask_persistsFilterSnapshotAndQueuesTask() {
        arrangeInsertedUuid("task-inventory-1");
        DeliveryInventoryFinishQuery query = new DeliveryInventoryFinishQuery();
        query.setCustomerUuid("customer-1");
        query.setKeyword("测试");
        DeliveryInventoryExportTaskCreateDTO dto = new DeliveryInventoryExportTaskCreateDTO();
        dto.setRequestId("inventory-request-1");
        dto.setQuery(query);

        String taskUuid = service.createDeliveryInventoryTask(dto);

        assertTaskSnapshot(taskUuid, "task-inventory-1", Permissions.DELIVERY_VIEW,
                "customerUuid", "keyword");
    }

    @Test
    void createDeliveryListTask_persistsQueueFilterAndQueuesTask() {
        arrangeInsertedUuid("task-delivery-list-1");
        DeliveryQuery query = new DeliveryQuery();
        query.setDeliveryStatus(2);
        query.setCustomerUuid("customer-1");
        DeliveryListExportTaskCreateDTO dto = new DeliveryListExportTaskCreateDTO();
        dto.setRequestId("delivery-list-request-1");
        dto.setQuery(query);

        String taskUuid = service.createDeliveryListTask(dto);

        assertTaskSnapshot(taskUuid, "task-delivery-list-1", Permissions.DELIVERY_VIEW,
                "deliveryStatus", "customerUuid");
    }

    @Test
    void createReportTask_persistsDimensionAndQueuesTask() {
        arrangeInsertedUuid("task-report-1");
        ReportQuery query = new ReportQuery();
        query.setDimension("customer");
        query.setCustomerUuid("customer-1");
        ReportExportTaskCreateDTO dto = new ReportExportTaskCreateDTO();
        dto.setRequestId("report-request-1");
        dto.setQuery(query);

        String taskUuid = service.createReportTask(dto);

        assertTaskSnapshot(taskUuid, "task-report-1", Permissions.REPORT_VIEW,
                "dimension", "customerUuid");
    }

    @Test
    void createReportTask_whenStorageUnavailable_rejectsBeforePersistingTask() {
        doThrow(new com.paper.mes.common.BusinessException("storage unavailable"))
                .when(storage).assertReadyForWrite();
        ReportExportTaskCreateDTO dto = new ReportExportTaskCreateDTO();
        dto.setRequestId("report-storage-failure");
        dto.setQuery(new ReportQuery());

        assertThatThrownBy(() -> service.createReportTask(dto))
                .isInstanceOf(com.paper.mes.common.BusinessException.class)
                .hasMessage("storage unavailable");
        org.mockito.Mockito.verify(taskMapper, org.mockito.Mockito.never()).insert(any(ExportTask.class));
    }

    private void arrangeInsertedUuid(String taskUuid) {
        when(taskMapper.insert(any(ExportTask.class))).thenAnswer(invocation -> {
            invocation.<ExportTask>getArgument(0).setUuid(taskUuid);
            return 1;
        });
    }

    private ExportTaskCreateDTO request(String requestId) {
        ExportTaskCreateDTO dto = new ExportTaskCreateDTO();
        dto.setRequestId(requestId);
        return dto;
    }

    private void assertDocumentTask(String actualUuid, String expectedUuid, String permission,
                                    String taskType, String moduleCode, String sourceUuid) {
        assertThat(actualUuid).isEqualTo(expectedUuid);
        verify(permissionChecker).require(permission);
        verify(taskExecutor).submit(expectedUuid);
        ArgumentCaptor<ExportTask> taskCaptor = ArgumentCaptor.forClass(ExportTask.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue()).extracting(ExportTask::getTaskType,
                        ExportTask::getModuleCode, ExportTask::getSourceUuid)
                .containsExactly(taskType, moduleCode, sourceUuid);
    }

    private void assertTaskSnapshot(String actualUuid, String expectedUuid,
                                    String permission, String... fields) {
        assertThat(actualUuid).isEqualTo(expectedUuid);
        verify(permissionChecker).require(permission);
        verify(taskExecutor).submit(expectedUuid);
        ArgumentCaptor<ExportTask> taskCaptor = ArgumentCaptor.forClass(ExportTask.class);
        verify(taskMapper, times(1)).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getRequestPayload()).contains(fields);
    }
}
