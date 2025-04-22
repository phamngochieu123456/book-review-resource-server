package com.hieupn.book_review.aspect;

import com.hieupn.book_review.config.PerformanceMonitoringConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Aspect for monitoring method execution time.
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final PerformanceMonitoringConfig config;

    public PerformanceMonitoringAspect(PerformanceMonitoringConfig config) {
        this.config = config;
    }

    /**
     * Pointcut that matches all repositories, services and controllers.
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)" +
            " || within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.stereotype.Controller *)" +
            " || within(@org.springframework.web.bind.annotation.RestController *)")
    public void springBeanPointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Pointcut that matches all Spring beans in the application's main packages.
     */
    @Pointcut("within(com.hieupn.book_review..*)")
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Pointcut for methods annotated with @Monitor
     */
    @Pointcut("@annotation(com.hieupn.book_review.aspect.Monitor)")
    public void monitoredMethodPointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Pointcut for classes annotated with @Monitor
     */
    @Pointcut("@within(com.hieupn.book_review.aspect.Monitor)")
    public void monitoredClassPointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Around advice that monitors the execution time of any Spring Bean in the application.
     * This covers all Spring components including repositories, services, and controllers.
     */
    @Around("(springBeanPointcut() && applicationPackagePointcut()) || monitoredMethodPointcut() || monitoredClassPointcut()")
    public Object monitorMethodExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!config.isMonitoringEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        // Get monitor annotation if present
        Monitor monitorMethod = method.getAnnotation(Monitor.class);
        Monitor monitorClass = method.getDeclaringClass().getAnnotation(Monitor.class);

        boolean logParams = determineLogParameters(monitorMethod, monitorClass);
        long threshold = determineThreshold(monitorMethod, monitorClass);
        String operationName = determineOperationName(joinPoint, monitorMethod);

        long startTime = System.currentTimeMillis();
        try {
            // Proceed with the method execution
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // Only log if execution time exceeds threshold
            if (executionTime >= threshold) {
                logExecutionTime(joinPoint, operationName, executionTime, logParams);
            }
        }
    }

    private boolean determineLogParameters(Monitor methodAnnotation, Monitor classAnnotation) {
        if (methodAnnotation != null && methodAnnotation.logParameters()) {
            return true;
        }
        if (classAnnotation != null && classAnnotation.logParameters()) {
            return true;
        }
        return config.isLogParameters();
    }

    private long determineThreshold(Monitor methodAnnotation, Monitor classAnnotation) {
        if (methodAnnotation != null && methodAnnotation.thresholdMillis() >= 0) {
            return methodAnnotation.thresholdMillis();
        }
        if (classAnnotation != null && classAnnotation.thresholdMillis() >= 0) {
            return classAnnotation.thresholdMillis();
        }
        return config.getThresholdMillis();
    }

    private String determineOperationName(ProceedingJoinPoint joinPoint, Monitor monitorAnnotation) {
        if (monitorAnnotation != null && !monitorAnnotation.name().isEmpty()) {
            return monitorAnnotation.name();
        }
        return joinPoint.getSignature().toShortString();
    }

    private void logExecutionTime(ProceedingJoinPoint joinPoint, String operationName,
                                  long executionTime, boolean logParams) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        StringBuilder logMessage = new StringBuilder()
                .append("PERFORMANCE: ")
                .append(className)
                .append(".")
                .append(methodName);

        if (logParams) {
            String args = Arrays.stream(joinPoint.getArgs())
                    .map(arg -> arg != null ? arg.toString() : "null")
                    .collect(Collectors.joining(", ", "[", "]"));
            logMessage.append(" with args: ").append(args);
        }

        logMessage.append(" executed in ").append(executionTime).append("ms");

        if (executionTime > 1000) {
            log.warn(logMessage.toString());
        } else {
            log.info(logMessage.toString());
        }
    }
}
