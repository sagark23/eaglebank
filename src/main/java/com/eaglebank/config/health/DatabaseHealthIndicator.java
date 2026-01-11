package com.eaglebank.config.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            // Test database connectivity with a simple query
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            if (result != null && result == 1) {
                Long userCount = getUserCount();
                Long accountCount = getAccountCount();
                Long transactionCount = getTransactionCount();

                return Health.up()
                        .withDetail("database", "H2 (In-Memory)")
                        .withDetail("status", "Connected")
                        .withDetail("userCount", userCount)
                        .withDetail("accountCount", accountCount)
                        .withDetail("transactionCount", transactionCount)
                        .build();
            } else {
                return Health.down()
                        .withDetail("reason", "Database query returned unexpected result")
                        .build();
            }

        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("type", e.getClass().getSimpleName())
                    .build();
        }
    }

    private Long getUserCount() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        } catch (Exception e) {
            log.warn("Could not get user count: {}", e.getMessage());
            return -1L;
        }
    }

    private Long getAccountCount() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bank_accounts", Long.class);
        } catch (Exception e) {
            log.warn("Could not get account count: {}", e.getMessage());
            return -1L;
        }
    }

    private Long getTransactionCount() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long.class);
        } catch (Exception e) {
            log.warn("Could not get transaction count: {}", e.getMessage());
            return -1L;
        }
    }
}

