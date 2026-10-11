package com.placement.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Startup configuration runner.
 * Clean slate configuration with no fake/mock demonstration records.
 * The portal starts fresh, ready for real student and company data entry.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // No fake data seeded on startup.
    }
}
