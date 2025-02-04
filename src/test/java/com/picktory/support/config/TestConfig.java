package com.picktory.support.config;

import com.picktory.support.DatabaseCleaner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    @Bean
    public DatabaseCleaner databaseCleaner() {
        return new DatabaseCleaner();
    }
}