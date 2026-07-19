package com.paper.mes.exporttask.service;

import jakarta.annotation.PreDestroy;
import com.paper.mes.exporttask.config.ExportTaskExecutorProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ExportTaskExecutor {
    private final ExportTaskWorker taskWorker;
    private final ThreadPoolExecutor executor;
    private final Counter rejectedCounter;
    private final int shutdownWaitSeconds;
    private final Set<String> submittedTasks = ConcurrentHashMap.newKeySet();

    public ExportTaskExecutor(ExportTaskWorker taskWorker, ExportTaskExecutorProperties properties,
                              MeterRegistry meterRegistry) {
        this.taskWorker = taskWorker;
        this.shutdownWaitSeconds = properties.getShutdownWaitSeconds();
        this.executor = createExecutor(properties);
        this.rejectedCounter = Counter.builder("paper_mes_export_executor_rejected")
                .description("Rejected export task executor submissions")
                .register(meterRegistry);
        registerGauges(meterRegistry);
    }

    public boolean submit(String taskUuid) {
        if (!submittedTasks.add(taskUuid)) return false;
        try {
            executor.execute(() -> executeTask(taskUuid));
            return true;
        } catch (RejectedExecutionException exception) {
            submittedTasks.remove(taskUuid);
            rejectedCounter.increment();
            log.warn("Export task executor capacity reached; task remains queued: {}", taskUuid);
            return false;
        }
    }

    public ExportTaskExecutorSnapshot snapshot() {
        return new ExportTaskExecutorSnapshot(
                executor.getMaximumPoolSize(), executor.getActiveCount(), executor.getQueue().size(),
                executor.getQueue().size() + executor.getQueue().remainingCapacity(),
                Math.round(rejectedCounter.count()), executor.getCompletedTaskCount());
    }

    private void executeTask(String taskUuid) {
        try {
            taskWorker.execute(taskUuid);
        } catch (RuntimeException exception) {
            log.error("Export task worker crashed before completing task: {}", taskUuid, exception);
        } finally {
            submittedTasks.remove(taskUuid);
        }
    }

    private ThreadPoolExecutor createExecutor(ExportTaskExecutorProperties properties) {
        int workers = properties.getWorkerCount();
        return new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                Thread.ofPlatform().name("export-task-worker-", 1).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private void registerGauges(MeterRegistry registry) {
        Gauge.builder("paper_mes_export_executor_active", executor, ThreadPoolExecutor::getActiveCount)
                .description("Active export task worker threads").register(registry);
        Gauge.builder("paper_mes_export_executor_queued", executor, item -> item.getQueue().size())
                .description("Export tasks queued in process memory").register(registry);
        Gauge.builder("paper_mes_export_executor_completed", executor, ThreadPoolExecutor::getCompletedTaskCount)
                .description("Export task executor completions since startup").register(registry);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownWaitSeconds, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
