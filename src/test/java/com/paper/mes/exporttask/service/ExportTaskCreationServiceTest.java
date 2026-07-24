package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTaskCreationServiceTest {
    private ExportTaskMapper taskMapper;
    private ExportTaskExecutor taskExecutor;
    private ExportTaskDocumentResolver documentResolver;
    private PermissionChecker permissionChecker;
    private ExportTaskStorage storage;
    private DeliveryOrderExportRevisionSnapshot deliveryOrderRevisionSnapshot;
    private ProcessOrderExportRevisionSnapshot processOrderRevisionSnapshot;
    private DeliveryExportSnapshotTaskCreator deliveryExportSnapshotTaskCreator;
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
        deliveryOrderRevisionSnapshot = mock(DeliveryOrderExportRevisionSnapshot.class);
        processOrderRevisionSnapshot = mock(ProcessOrderExportRevisionSnapshot.class);
        deliveryExportSnapshotTaskCreator = mock(DeliveryExportSnapshotTaskCreator.class);
        service = new ExportTaskCreationService(taskMapper, taskExecutor, mock(ExportTaskLifecycleService.class),
                documentResolver, permissionChecker, new ExportTaskPayloadWriter(new com.fasterxml.jackson.databind.ObjectMapper()), storage,
                mock(com.paper.mes.report.service.ReportQuerySnapshotService.class),
                deliveryOrderRevisionSnapshot, processOrderRevisionSnapshot, deliveryExportSnapshotTaskCreator);
        org.mockito.Mockito.doNothing().when(storage).assertReadyForWrite();
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1")
                .username("operator").realName("Operator").build());
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
        when(processOrderRevisionSnapshot.capture("order-1", null)).thenReturn("{\"customerRevisionNo\":0}");
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
        when(deliveryOrderRevisionSnapshot.capture("delivery-1", null, null)).thenReturn("{\"customerRevisionNo\":0}");
        arrangeInsertedUuid("task-delivery-1");

        String taskUuid = service.createDeliveryOrderTask("delivery-1", request("request-delivery-1"));

        assertDocumentTask(taskUuid, "task-delivery-1", Permissions.DELIVERY_VIEW,
                DeliveryOrderDetailExportTaskHandler.TASK_TYPE, "delivery", "delivery-1");
    }

    @Test
    void createDeliveryInventoryTask_persistsFilterSnapshotAndQueuesTask() {
        when(deliveryExportSnapshotTaskCreator.createInventory(any(), any(), any(), any()))
                .thenReturn("task-inventory-1");
        DeliveryInventoryFinishQuery query = new DeliveryInventoryFinishQuery();
        query.setCustomerUuid("customer-1");
        query.setKeyword("娴嬭瘯");
        DeliveryInventoryExportTaskCreateDTO dto = new DeliveryInventoryExportTaskCreateDTO();
        dto.setRequestId("inventory-request-1");
        dto.setQuery(query);

        String taskUuid = service.createDeliveryInventoryTask(dto);

        assertThat(taskUuid).isEqualTo("task-inventory-1");
        verify(permissionChecker).require(Permissions.DELIVERY_VIEW);
        verify(deliveryExportSnapshotTaskCreator).createInventory(
                eq("inventory-request-1"), contains("customerUuid"), any(), eq(query));
    }

    @Test
    void createDeliveryListTask_persistsQueueFilterAndQueuesTask() {
        when(deliveryExportSnapshotTaskCreator.createReconciliation(any(), any(), any(), any()))
                .thenReturn("task-delivery-list-1");
        DeliveryQuery query = new DeliveryQuery();
        query.setDeliveryStatus(2);
        query.setCustomerUuid("customer-1");
        DeliveryListExportTaskCreateDTO dto = new DeliveryListExportTaskCreateDTO();
        dto.setRequestId("delivery-list-request-1");
        dto.setQuery(query);

        String taskUuid = service.createDeliveryListTask(dto);

        assertThat(taskUuid).isEqualTo("task-delivery-list-1");
        verify(permissionChecker).require(Permissions.DELIVERY_VIEW);
        verify(deliveryExportSnapshotTaskCreator).createReconciliation(
                eq("delivery-list-request-1"), contains("deliveryStatus"), any(), eq(query));
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
