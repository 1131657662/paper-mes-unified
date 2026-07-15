package com.paper.mes.backup.service;

import com.paper.mes.backup.entity.BackupTask;
import com.paper.mes.backup.mapper.BackupTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupTaskHistoryServiceTest {

    @Test
    void start_createsRunningTask() {
        BackupTaskMapper mapper = mock(BackupTaskMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        doAnswer(invocation -> assignUuid(invocation.getArgument(0)))
                .when(mapper).insert(any(BackupTask.class));
        BackupTaskHistoryService service = new BackupTaskHistoryService(mapper, publisher);

        String taskUuid = service.start("BACKUP", null, "admin");

        assertEquals("task-uuid", taskUuid);
        verify(mapper).insert(org.mockito.ArgumentMatchers.<BackupTask>argThat(task -> "RUNNING".equals(task.getTaskStatus())
                && "BACKUP".equals(task.getTaskType())
                && "admin".equals(task.getOperator())
                && task.getStartedAt() != null));
    }

    @Test
    void finish_withSuccessfulTask_recordsCompletion() {
        BackupTaskMapper mapper = mock(BackupTaskMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        BackupTask task = runningTask();
        when(mapper.selectById("task-uuid")).thenReturn(task);
        when(mapper.updateById(task)).thenReturn(1);
        BackupTaskHistoryService service = new BackupTaskHistoryService(mapper, publisher);

        service.finish("task-uuid", "20260713-023000", true, "备份完成");

        assertEquals("SUCCESS", task.getTaskStatus());
        assertEquals("20260713-023000", task.getBackupId());
        assertEquals("备份完成", task.getMessage());
        assertNotNull(task.getFinishedAt());
        assertTrue(task.getDurationMs() >= 0);
    }

    @Test
    void finish_withFailedTask_recordsSafeMessage() {
        BackupTaskMapper mapper = mock(BackupTaskMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        BackupTask task = runningTask();
        task.setTaskType("BACKUP");
        when(mapper.selectById("task-uuid")).thenReturn(task);
        when(mapper.updateById(task)).thenReturn(1);
        BackupTaskHistoryService service = new BackupTaskHistoryService(mapper, publisher);

        service.finish("task-uuid", null, false, "任务失败，请查看服务器日志");

        assertEquals("FAILED", task.getTaskStatus());
        assertEquals("任务失败，请查看服务器日志", task.getMessage());
        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.<BackupTaskFailedEvent>argThat(event ->
                "task-uuid".equals(event.taskUuid()) && "BACKUP".equals(event.taskType())));
    }

    @Test
    void automaticHistory_withFailuresAfterSuccess_countsCurrentFailureStreak() {
        BackupTaskMapper mapper = mock(BackupTaskMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                automaticTask("FAILED", 1),
                automaticTask("FAILED", 2),
                automaticTask("SUCCESS", 3),
                automaticTask("FAILED", 4)));
        BackupTaskHistoryService service = new BackupTaskHistoryService(mapper, publisher);

        BackupAutomaticHistory history = service.automaticHistory();

        assertEquals("FAILED", history.lastStatus());
        assertEquals(2, history.consecutiveFailures());
    }

    @Test
    void recoverInterruptedTasks_marksRunningTasksFailedAndPublishesFailure() {
        BackupTaskMapper mapper = mock(BackupTaskMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        BackupTask task = runningTask();
        task.setTaskType("VERIFY");
        task.setBackupId("20260714-020000");
        task.setVersion(1);
        when(mapper.selectList(any())).thenReturn(List.of(task));
        when(mapper.updateById(task)).thenReturn(1);
        BackupTaskHistoryService service = new BackupTaskHistoryService(mapper, publisher);

        int recovered = service.recoverInterruptedTasks();

        assertEquals(1, recovered);
        assertEquals("FAILED", task.getTaskStatus());
        assertNotNull(task.getFinishedAt());
        assertTrue(task.getDurationMs() >= 0);
        assertTrue(task.getMessage().contains("服务重启"));
        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.<BackupTaskFailedEvent>argThat(event ->
                task.getUuid().equals(event.taskUuid()) && "VERIFY".equals(event.taskType())));
    }

    private int assignUuid(BackupTask task) {
        task.setUuid("task-uuid");
        return 1;
    }

    private BackupTask runningTask() {
        BackupTask task = new BackupTask();
        task.setUuid("task-uuid");
        task.setTaskStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now().minusSeconds(1));
        return task;
    }

    private BackupTask automaticTask(String status, int daysAgo) {
        BackupTask task = new BackupTask();
        task.setTaskType("AUTO_BACKUP");
        task.setTaskStatus(status);
        task.setStartedAt(LocalDateTime.now().minusDays(daysAgo));
        return task;
    }
}
