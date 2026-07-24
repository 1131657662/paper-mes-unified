package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.dto.ReportExportTaskCreateDTO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQuerySnapshotVO;
import com.paper.mes.report.service.ReportQuerySnapshotBundle;
import com.paper.mes.report.service.ReportQuerySnapshotService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportTaskCreationServiceTest {
    private ExportTaskMapper taskMapper;
    private ExportTaskExecutor taskExecutor;
    private PermissionChecker permissionChecker;
    private ExportTaskStorage storage;
    private ReportQuerySnapshotService snapshotService;
    private ExportTaskCreationService service;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(ExportTaskMapper.class);
        taskExecutor = mock(ExportTaskExecutor.class);
        permissionChecker = mock(PermissionChecker.class);
        storage = mock(ExportTaskStorage.class);
        snapshotService = mock(ReportQuerySnapshotService.class);
        service = new ExportTaskCreationService(taskMapper, taskExecutor, mock(ExportTaskLifecycleService.class),
                mock(ExportTaskDocumentResolver.class), permissionChecker,
                new ExportTaskPayloadWriter(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()), storage,
                snapshotService,
                mock(DeliveryOrderExportRevisionSnapshot.class),
                mock(ProcessOrderExportRevisionSnapshot.class));
        org.mockito.Mockito.doNothing().when(storage).assertReadyForWrite();
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1")
                .username("operator").realName("Operator").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
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
        dto.setReportPath("/reports/explorer");
        arrangeSnapshot(query, "release-1");

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
        dto.setReportPath("/reports/overview");
        arrangeSnapshot(dto.getQuery(), "release-1");

        assertThatThrownBy(() -> service.createReportTask(dto))
                .isInstanceOf(com.paper.mes.common.BusinessException.class)
                .hasMessage("storage unavailable");
        org.mockito.Mockito.verify(taskMapper, org.mockito.Mockito.never()).insert(any(ExportTask.class));
    }

    @Test
    void createScheduledReportTask_forEligibleRecipient_usesSubscriptionAsSource() {
        arrangeInsertedUuid("task-scheduled-1");
        CurrentUser recipient = CurrentUser.builder().uuid("recipient-1").username("finance")
                .realName("Finance").roleCode("finance").build();
        when(permissionChecker.hasRolePermission("finance", Permissions.REPORT_VIEW)).thenReturn(true);
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid("release-1");
        arrangeSnapshot(query, "release-1");

        String taskUuid = service.createScheduledReportTask("scheduled-request", "subscription-1",
                "Daily processing report", "/reports/production", query, recipient);

        assertThat(taskUuid).isEqualTo("task-scheduled-1");
        ArgumentCaptor<ExportTask> captor = ArgumentCaptor.forClass(ExportTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue()).extracting(ExportTask::getOperationCode,
                        ExportTask::getSourceUuid, ExportTask::getRequesterUuid,
                        ExportTask::getSourcePath, ExportTask::getMetricReleaseUuid)
                .containsExactly("scheduled-export", "subscription-1", "recipient-1",
                        "/reports/production", "release-1");
        assertThat(captor.getValue().getRequestPayload()).contains("release-1");
    }

    private void arrangeInsertedUuid(String taskUuid) {
        when(taskMapper.insert(any(ExportTask.class))).thenAnswer(invocation -> {
            invocation.<ExportTask>getArgument(0).setUuid(taskUuid);
            return 1;
        });
    }

    private void arrangeSnapshot(ReportQuery query, String releaseUuid) {
        query.setMetricReleaseUuid(releaseUuid);
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 12, 0);
        ReportQuerySnapshotVO snapshot = new ReportQuerySnapshotVO(
                "snapshot-1", "snapshot-1", "hash-1", releaseUuid,
                Map.of("order_count", "version-1"), now, now, now.plusDays(8),
                "scope", "LIVE_DB_READ", "LIVE_ONLY", List.of(), Map.of());
        when(snapshotService.createForExport(any(ReportQuery.class), any(CurrentUser.class)))
                .thenReturn(new ReportQuerySnapshotBundle(query, snapshot));
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
