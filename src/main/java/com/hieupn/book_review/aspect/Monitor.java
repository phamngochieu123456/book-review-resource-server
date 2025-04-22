package com.hieupn.book_review.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for detailed performance monitoring.
 * This allows for more granular control over which methods are monitored
 * and can be used to override global threshold settings.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {
    /**
     * Threshold in milliseconds for logging execution time.
     * Only log if execution time exceeds this value.
     * Default is -1 which means use the global setting.
     */
    long thresholdMillis() default -1;

    /**
     * Whether to include method parameters in the log.
     * Default is to use the global setting.
     */
    boolean logParameters() default false;

    /**
     * Custom name for the monitoring log entry.
     * If not specified, the method name will be used.
     */
    String name() default "";
}
