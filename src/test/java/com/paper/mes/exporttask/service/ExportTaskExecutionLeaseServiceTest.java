package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.exporttask.config.ExportTaskRuntimeProperties;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTaskExecutionLeaseServiceTest {
    private ExportTaskMapper taskMapper;
    private ExportTaskExecutionLeaseService service;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(ExportTaskMapper.class);
        service = new ExportTaskExecutionLeaseService(taskMapper, properties());
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void claim_whenTaskIsQueued_assignsUniqueExecutionToken() {
        ExportTask task = task();
        when(taskMapper.selectById(task.getUuid())).thenReturn(task);
        when(taskMapper.update(isNull(), any())).thenReturn(1);

        ExportTaskExecutionLease lease = service.claim(task.getUuid()).orElseThrow();

        assertThat(lease.token()).hasSize(36);
        LambdaUpdateWrapper<ExportTask> update = capturedUpdate();
        assertThat(update.getCustomSqlSegment()).contains("uuid", "task_status");
        assertThat(update.getParamNameValuePairs()).containsValues(task.getUuid(), lease.token(), 1, 2);
    }

    @Test
    void renew_scopesHeartbeatToOwnedRunningExecution() {
        ExportTaskExecutionLease lease = lease();
        when(taskMapper.update(isNull(), any())).thenReturn(1);

        assertThat(service.renew(lease)).isTrue();

        assertOwnedRunningFence(capturedUpdate(), lease);
    }

    @Test
    void markFailure_scopesTerminalUpdateToOwnedRunningExecution() {
        ExportTaskExecutionLease lease = lease();
        when(taskMapper.update(isNull(), any())).thenReturn(1);

        assertThat(service.markFailure(lease, "failed")).isTrue();

        LambdaUpdateWrapper<ExportTask> update = capturedUpdate();
        assertOwnedRunningFence(update, lease);
        assertThat(update.getParamNameValuePairs()).containsValue("failed");
    }

    @Test
    void markSuccess_whenLeaseWasRecovered_cannotCompleteNewExecution(@TempDir Path tempDir) throws Exception {
        ExportTaskExecutionLease staleLease = lease();
        Path artifact = Files.writeString(tempDir.resolve("task.xlsx"), "report");
        when(taskMapper.update(isNull(), any())).thenReturn(0);

        boolean updated = service.markSuccess(staleLease, artifact,
                new ExportTaskArtifact("task.xlsx", "application/xlsx"));

        assertThat(updated).isFalse();
        LambdaUpdateWrapper<ExportTask> update = capturedUpdate();
        assertOwnedRunningFence(update, staleLease);
        assertThat(update.getParamNameValuePairs()).containsValue("task.xlsx");
        assertThat(update.getParamNameValuePairs()).doesNotContainValue(artifact.toString());
    }

    @Test
    void startHeartbeat_renewsLeaseUntilHandleIsClosed() throws Exception {
        CountDownLatch renewed = new CountDownLatch(1);
        when(taskMapper.update(isNull(), any())).thenAnswer(invocation -> {
            renewed.countDown();
            return 1;
        });

        ExportTaskHeartbeat heartbeat = service.startHeartbeat(lease());

        assertThat(renewed.await(2, TimeUnit.SECONDS)).isTrue();
        heartbeat.close();
        verify(taskMapper, atLeastOnce()).update(isNull(), any());
    }

    private LambdaUpdateWrapper<ExportTask> capturedUpdate() {
        ArgumentCaptor<LambdaUpdateWrapper<ExportTask>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(taskMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    private void assertOwnedRunningFence(LambdaUpdateWrapper<ExportTask> update,
                                         ExportTaskExecutionLease lease) {
        assertThat(update.getCustomSqlSegment()).contains("uuid", "task_status", "worker_id");
        assertThat(update.getParamNameValuePairs()).containsValues(lease.task().getUuid(), lease.token(), 2);
    }

    private ExportTaskExecutionLease lease() {
        return new ExportTaskExecutionLease(task(), "lease-token-1");
    }

    private ExportTask task() {
        ExportTask task = new ExportTask();
        task.setUuid("task-1");
        task.setTaskStatus(1);
        return task;
    }

    private ExportTaskRuntimeProperties properties() {
        ExportTaskRuntimeProperties properties = new ExportTaskRuntimeProperties();
        properties.setHeartbeatIntervalSeconds(1);
        return properties;
    }
}
