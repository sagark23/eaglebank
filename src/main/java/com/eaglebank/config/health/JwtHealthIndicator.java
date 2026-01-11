package com.eaglebank.config.health;

import com.eaglebank.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtHealthIndicator implements HealthIndicator {

    private final JwtConfig jwtConfig;

    @Override
    public Health health() {
        try {
            String secret = jwtConfig.getSecret();
            Long expiration = jwtConfig.getExpiration();

            if (secret == null || secret.trim().isEmpty()) {
                return Health.down()
                        .withDetail("reason", "JWT secret is not configured")
                        .build();
            }

            if (secret.length() < 32) {
                return Health.down()
                        .withDetail("reason", "JWT secret is too short (< 32 chars)")
                        .withDetail("currentLength", secret.length())
                        .build();
            }

            if (expiration == null || expiration <= 0) {
                return Health.down()
                        .withDetail("reason", "JWT expiration is invalid")
                        .build();
            }

            return Health.up()
                    .withDetail("secretLength", secret.length())
                    .withDetail("expirationMs", expiration)
                    .withDetail("expirationHours", expiration / (1000 * 60 * 60))
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

