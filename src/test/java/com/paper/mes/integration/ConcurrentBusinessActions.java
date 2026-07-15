package com.paper.mes.integration;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class ConcurrentBusinessActions {

    private ConcurrentBusinessActions() {
    }

    static <T> List<Outcome<T>> runPair(Callable<T> first, Callable<T> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome<T>> firstResult = executor.submit(() -> run(first, ready, start));
            Future<Outcome<T>> secondResult = executor.submit(() -> run(second, ready, start));
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent actions did not become ready");
            }
            start.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> Outcome<T> run(Callable<T> action, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            return new Outcome<>(action.call(), null);
        } catch (Throwable error) {
            return new Outcome<>(null, error);
        }
    }

    record Outcome<T>(T value, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }
}
