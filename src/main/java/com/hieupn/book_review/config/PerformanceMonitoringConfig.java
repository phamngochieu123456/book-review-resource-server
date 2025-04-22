package com.hieupn.book_review.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for performance monitoring settings
 */
@Configuration
@EnableAspectJAutoProxy
public class PerformanceMonitoringConfig {

    @Value("${performance.monitoring.enabled:false}")
    private boolean monitoringEnabled;

    @Value("${performance.monitoring.threshold.millis:0}")
    private long thresholdMillis;

    @Value("${performance.monitoring.log.parameters:false}")
    private boolean logParameters;

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public long getThresholdMillis() {
        return thresholdMillis;
    }

    public boolean isLogParameters() {
        return logParameters;
    }
}
