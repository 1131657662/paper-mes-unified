package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Component
class ProcessAiTaskExecutor {

    private static final int QUEUE_CAPACITY = 10;
    private final ThreadPoolExecutor executor;

    ProcessAiTaskExecutor(AiProperties properties) {
        int workers = properties.getGlobalConcurrentRequests();
        executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY), threadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    Future<?> submit(Runnable task) {
        try {
            return executor.submit(task);
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "AI_BUSY", "AI工艺助手当前繁忙，请稍后重试");
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private ThreadFactory threadFactory() {
        AtomicInteger number = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "process-ai-" + number.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
