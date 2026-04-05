package com.financeapp.finance_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestFinanceBackendApplication {

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalDevContainerConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }

    public static void main(String[] args) {
        SpringApplication
            .from(FinanceBackendApplication::main)
            .with(LocalDevContainerConfig.class)
            .run(args);
    }
}