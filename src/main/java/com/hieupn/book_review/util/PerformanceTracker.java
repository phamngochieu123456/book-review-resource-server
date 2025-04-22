package com.hieupn.book_review.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for manually tracking execution time of code blocks.
 * This is useful for measuring specific sections within a method.
 */
public class PerformanceTracker {
    private static final Logger log = LoggerFactory.getLogger(PerformanceTracker.class);
    private static final ThreadLocal<Map<String, Long>> startTimes = ThreadLocal.withInitial(HashMap::new);

    /**
     * Start tracking time for a named operation.
     *
     * @param operationName Name of the operation to track
     */
    public static void start(String operationName) {
        startTimes.get().put(operationName, System.nanoTime());
    }

    /**
     * Stop tracking and log the execution time for a named operation.
     *
     * @param operationName Name of the operation
     * @return The execution time in milliseconds
     */
    public static long stop(String operationName) {
        return stop(operationName, 0);
    }

    /**
     * Stop tracking and log the execution time for a named operation if
     * it exceeds the specified threshold.
     *
     * @param operationName Name of the operation
     * @param thresholdMillis Only log if execution time exceeds this threshold
     * @return The execution time in milliseconds
     */
    public static long stop(String operationName, long thresholdMillis) {
        Map<String, Long> times = startTimes.get();
        Long startTime = times.remove(operationName);

        if (startTime == null) {
            log.warn("PERFORMANCE: Operation '{}' was stopped but never started", operationName);
            return -1;
        }

        long executionTimeNanos = System.nanoTime() - startTime;
        long executionTimeMillis = TimeUnit.NANOSECONDS.toMillis(executionTimeNanos);

        if (executionTimeMillis >= thresholdMillis) {
            if (executionTimeMillis > 1000) {
                log.warn("PERFORMANCE: Operation '{}' executed in {}ms", operationName, executionTimeMillis);
            } else {
                log.info("PERFORMANCE: Operation '{}' executed in {}ms", operationName, executionTimeMillis);
            }
        }

        return executionTimeMillis;
    }

    /**
     * Clear all tracked operations for the current thread.
     * Should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        startTimes.remove();
    }
}
