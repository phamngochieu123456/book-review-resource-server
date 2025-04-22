package com.hieupn.book_review.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Custom health indicator for database connection pool
 * Provides detailed health information about the connection pool status
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

                if (poolMXBean != null) {
                    int total = poolMXBean.getTotalConnections();
                    int active = poolMXBean.getActiveConnections();
                    int idle = poolMXBean.getIdleConnections();
                    int waiting = poolMXBean.getThreadsAwaitingConnection();

                    Health.Builder builder = Health.up()
                            .withDetail("total", total)
                            .withDetail("active", active)
                            .withDetail("idle", idle)
                            .withDetail("waiting", waiting);

                    // If pool is at capacity with waiting connections, mark as DOWN
                    if (active >= total && waiting > 0) {
                        return builder.down()
                                .withDetail("message", "Connection pool at capacity with waiting requests")
                                .build();
                    }

                    return builder.build();
                }
            }

            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
