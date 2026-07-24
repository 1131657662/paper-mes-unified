package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportTaskMaintenanceServiceTest {
    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @Test
    void dispatchPending_countsOnlyTasksAcceptedByInMemoryExecutor() {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        ExportTaskExecutor executor = mock(ExportTaskExecutor.class);
        when(taskMapper.selectList(any())).thenReturn(List.of(task("task-1"), task("task-2")));
        when(executor.submit("task-1")).thenReturn(true);
        when(executor.submit("task-2")).thenReturn(false);
        ExportTaskMaintenanceService service = new ExportTaskMaintenanceService(
                taskMapper, mock(ExportTaskStorage.class), executor, mock(ExportTaskExpirationService.class));

        int dispatched = service.dispatchPending();

        assertThat(dispatched).isEqualTo(1);
    }

    @Test
    void recoverStaleRunning_clearsPreviousExecutionLease() {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        when(taskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            LambdaUpdateWrapper<ExportTask> update = invocation.getArgument(1);
            assertThat(update.getCustomSqlSegment()).contains("task_status", "heartbeat_at", "started_at");
            assertThat(update.getSqlSet()).contains("worker_id");
            return 1;
        });
        ExportTaskMaintenanceService service = new ExportTaskMaintenanceService(
                taskMapper, mock(ExportTaskStorage.class), mock(ExportTaskExecutor.class),
                mock(ExportTaskExpirationService.class));

        int recovered = service.recoverStaleRunning(LocalDateTime.now().minusMinutes(10));

        assertThat(recovered).isEqualTo(1);
    }

    @Test
    void cleanupOrphanArtifacts_deletesOldUnreferencedExecutionFile(@TempDir Path tempDir) throws Exception {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        Path artifact = storage.target(lease("task-1", "lease-1"), "xlsx");
        Files.writeString(artifact, "partial");
        Files.setLastModifiedTime(artifact, FileTime.from(cutoff.minusMinutes(1)
                .atZone(ZoneId.systemDefault()).toInstant()));
        when(taskMapper.selectList(any())).thenReturn(List.of(), List.of());
        ExportTaskMaintenanceService service = new ExportTaskMaintenanceService(
                taskMapper, storage, mock(ExportTaskExecutor.class), mock(ExportTaskExpirationService.class));

        int deleted = service.cleanupOrphanArtifacts(cutoff, 10);

        assertThat(deleted).isEqualTo(1);
        assertThat(Files.exists(artifact)).isFalse();
    }

    @Test
    void cleanupOrphanArtifacts_preservesReferencedAndRunningExecutionFiles(@TempDir Path tempDir) throws Exception {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        ExportTaskStorage storage = new ExportTaskStorage(tempDir.resolve("exports").toString());
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        Path referencedPath = storage.target(lease("task-1", "lease-1"), "xlsx");
        Path activePath = storage.target(lease("task-1", "lease-2"), "xlsx");
        Files.writeString(referencedPath, "referenced");
        Files.writeString(activePath, "active");
        FileTime oldTime = FileTime.from(cutoff.minusMinutes(1)
                .atZone(ZoneId.systemDefault()).toInstant());
        Files.setLastModifiedTime(referencedPath, oldTime);
        Files.setLastModifiedTime(activePath, oldTime);
        ExportTask referenced = task("task-1");
        referenced.setFilePath(storage.storageKey(referencedPath));
        ExportTask running = task("task-1");
        running.setTaskStatus(2);
        running.setWorkerId("lease-2");
        when(taskMapper.selectList(any())).thenReturn(List.of(referenced), List.of(running));
        ExportTaskMaintenanceService service = new ExportTaskMaintenanceService(
                taskMapper, storage, mock(ExportTaskExecutor.class), mock(ExportTaskExpirationService.class));

        int deleted = service.cleanupOrphanArtifacts(cutoff, 10);

        assertThat(deleted).isZero();
        assertThat(Files.exists(referencedPath)).isTrue();
        assertThat(Files.exists(activePath)).isTrue();
    }

    private ExportTask task(String uuid) {
        ExportTask task = new ExportTask();
        task.setUuid(uuid);
        return task;
    }

    private ExportTaskExecutionLease lease(String uuid, String token) {
        return new ExportTaskExecutionLease(task(uuid), token);
    }
}
