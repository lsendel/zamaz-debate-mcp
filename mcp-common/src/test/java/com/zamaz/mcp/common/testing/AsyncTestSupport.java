package com.zamaz.mcp.common.testing;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utilities for testing asynchronous operations.
 */
public class AsyncTestSupport {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

    /**
     * Waits for a condition to become true within the default timeout.
     */
    public static void awaitCondition(Supplier<Boolean> condition) {
        awaitCondition(condition, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for a condition to become true within the specified timeout.
     */
    public static void awaitCondition(Supplier<Boolean> condition, Duration timeout) {
        Awaitility.await()
            .atMost(timeout)
            .pollInterval(DEFAULT_POLL_INTERVAL)
            .until(condition::get);
    }

    /**
     * Waits for a future to complete and returns its value.
     */
    public static <T> T awaitFuture(Future<T> future) {
        return awaitFuture(future, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for a future to complete within the specified timeout and returns its value.
     */
    public static <T> T awaitFuture(Future<T> future, Duration timeout) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for future", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Future execution failed", e.getCause());
        } catch (TimeoutException e) {
            throw new ConditionTimeoutException("Future did not complete within " + timeout);
        }
    }

    /**
     * Waits for a CompletableFuture to complete and returns its value.
     */
    public static <T> T awaitCompletableFuture(CompletableFuture<T> future) {
        return awaitCompletableFuture(future, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for a CompletableFuture to complete within the specified timeout and returns its value.
     */
    public static <T> T awaitCompletableFuture(CompletableFuture<T> future, Duration timeout) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for future", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Future execution failed", e.getCause());
        } catch (TimeoutException e) {
            throw new ConditionTimeoutException("Future did not complete within " + timeout);
        }
    }

    /**
     * Executes an action asynchronously and waits for it to complete.
     */
    public static void awaitAsync(Runnable action) {
        awaitAsync(action, DEFAULT_TIMEOUT);
    }

    /**
     * Executes an action asynchronously and waits for it to complete within the specified timeout.
     */
    public static void awaitAsync(Runnable action, Duration timeout) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(action);
        awaitCompletableFuture(future, timeout);
    }

    /**
     * Executes a supplier asynchronously and returns its value.
     */
    public static <T> T awaitAsyncResult(Supplier<T> supplier) {
        return awaitAsyncResult(supplier, DEFAULT_TIMEOUT);
    }

    /**
     * Executes a supplier asynchronously and returns its value within the specified timeout.
     */
    public static <T> T awaitAsyncResult(Supplier<T> supplier, Duration timeout) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier);
        return awaitCompletableFuture(future, timeout);
    }

    /**
     * Waits for all futures to complete and returns their values.
     */
    @SafeVarargs
    public static <T> List<T> awaitAllFutures(CompletableFuture<T>... futures) {
        return awaitAllFutures(DEFAULT_TIMEOUT, futures);
    }

    /**
     * Waits for all futures to complete within the specified timeout and returns their values.
     */
    @SafeVarargs
    public static <T> List<T> awaitAllFutures(Duration timeout, CompletableFuture<T>... futures) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        awaitCompletableFuture(allOf, timeout);
        
        return java.util.Arrays.stream(futures)
            .map(CompletableFuture::join)
            .toList();
    }

    /**
     * Repeatedly executes an action until it succeeds or the timeout is reached.
     */
    public static void retryUntilSuccess(Runnable action) {
        retryUntilSuccess(action, DEFAULT_TIMEOUT);
    }

    /**
     * Repeatedly executes an action until it succeeds or the timeout is reached.
     */
    public static void retryUntilSuccess(Runnable action, Duration timeout) {
        Awaitility.await()
            .atMost(timeout)
            .pollInterval(DEFAULT_POLL_INTERVAL)
            .ignoreExceptions()
            .untilAsserted(action::run);
    }

    /**
     * Creates a countdown latch helper for testing concurrent operations.
     */
    public static CountDownLatchHelper countDownLatch(int count) {
        return new CountDownLatchHelper(count);
    }

    /**
     * Helper class for working with CountDownLatch in tests.
     */
    public static class CountDownLatchHelper {
        private final CountDownLatch latch;

        public CountDownLatchHelper(int count) {
            this.latch = new CountDownLatch(count);
        }

        public void countDown() {
            latch.countDown();
        }

        public void await() {
            await(DEFAULT_TIMEOUT);
        }

        public void await(Duration timeout) {
            try {
                if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new ConditionTimeoutException("Latch did not reach zero within " + timeout);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for latch", e);
            }
        }

        public long getCount() {
            return latch.getCount();
        }
    }

    /**
     * Creates a test executor service with a fixed thread pool.
     */
    public static ExecutorService testExecutor(int threads) {
        return new TestExecutorService(threads);
    }

    /**
     * Test executor service that tracks submitted tasks and provides cleanup.
     */
    private static class TestExecutorService extends ThreadPoolExecutor {
        public TestExecutorService(int threads) {
            super(threads, threads, 0L, TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<>(),
                  new ThreadFactory() {
                      private int counter = 0;
                      @Override
                      public Thread newThread(Runnable r) {
                          Thread thread = new Thread(r);
                          thread.setName("test-executor-" + counter++);
                          thread.setDaemon(true);
                          return thread;
                      }
                  });
        }

        public void shutdownAndAwait() {
            shutdownAndAwait(DEFAULT_TIMEOUT);
        }

        public void shutdownAndAwait(Duration timeout) {
            shutdown();
            try {
                if (!awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    shutdownNow();
                    throw new ConditionTimeoutException("Executor did not terminate within " + timeout);
                }
            } catch (InterruptedException e) {
                shutdownNow();
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for executor termination", e);
            }
        }
    }
}