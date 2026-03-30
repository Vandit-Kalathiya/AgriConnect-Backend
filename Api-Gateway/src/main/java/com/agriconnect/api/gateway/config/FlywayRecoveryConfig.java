package com.agriconnect.api.gateway.config;

import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRecoveryConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRecoveryConfig.class);

    @Value("${FLYWAY_AUTO_REPAIR_ON_VALIDATION_ERROR:true}")
    private boolean autoRepairOnValidationError;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (Exception ex) {
                if (autoRepairOnValidationError && hasValidateException(ex)) {
                    log.warn("Flyway validation failed. Attempting automatic repair and retry.");
                    flyway.repair();
                    flyway.migrate();
                    return;
                }
                throw ex;
            }
        };
    }

    private boolean hasValidateException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof FlywayValidateException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
