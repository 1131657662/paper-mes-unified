package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.config.ExportTaskExecutorProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ExportTaskExecutorTest {
    private ExportTaskExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) executor.shutdown();
    }

    @Test
    void submit_whenCapacityIsFull_keepsRejectedTaskEligibleForLaterSubmission() throws Exception {
        ExportTaskWorker worker = mock(ExportTaskWorker.class);
        ControlledWorker controlled = new ControlledWorker();
        doAnswer(invocation -> controlled.execute(invocation.getArgument(0)))
                .when(worker).execute(org.mockito.ArgumentMatchers.any());
        executor = new ExportTaskExecutor(worker, properties(1, 1), new SimpleMeterRegistry());

        assertThat(executor.submit("task-1")).isTrue();
        assertThat(controlled.firstStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.submit("task-2")).isTrue();
        assertThat(executor.submit("task-3")).isFalse();

        ExportTaskExecutorSnapshot saturated = executor.snapshot();
        assertThat(saturated.activeWorkerCount()).isEqualTo(1);
        assertThat(saturated.queuedInMemoryCount()).isEqualTo(1);
        assertThat(saturated.queueCapacity()).isEqualTo(1);
        assertThat(saturated.rejectedSubmissionCount()).isEqualTo(1);

        controlled.releaseFirst.countDown();
        assertThat(controlled.secondCompleted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.submit("task-3")).isTrue();
        assertThat(controlled.thirdCompleted.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void submit_whenTaskAlreadyScheduled_doesNotCountDuplicateAsCapacityRejection() throws Exception {
        ExportTaskWorker worker = mock(ExportTaskWorker.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return null;
        }).when(worker).execute("task-1");
        executor = new ExportTaskExecutor(worker, properties(1, 1), new SimpleMeterRegistry());

        assertThat(executor.submit("task-1")).isTrue();
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.submit("task-1")).isFalse();
        assertThat(executor.snapshot().rejectedSubmissionCount()).isZero();
        release.countDown();
    }

    private ExportTaskExecutorProperties properties(int workers, int queueCapacity) {
        ExportTaskExecutorProperties properties = new ExportTaskExecutorProperties();
        properties.setWorkerCount(workers);
        properties.setQueueCapacity(queueCapacity);
        properties.setShutdownWaitSeconds(1);
        return properties;
    }

    private static final class ControlledWorker {
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);
        private final CountDownLatch secondCompleted = new CountDownLatch(1);
        private final CountDownLatch thirdCompleted = new CountDownLatch(1);

        private Object execute(String taskUuid) throws InterruptedException {
            if ("task-1".equals(taskUuid)) {
                firstStarted.countDown();
                releaseFirst.await(2, TimeUnit.SECONDS);
            }
            if ("task-2".equals(taskUuid)) secondCompleted.countDown();
            if ("task-3".equals(taskUuid)) thirdCompleted.countDown();
            return null;
        }
    }
}
