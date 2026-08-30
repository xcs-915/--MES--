package com.tns.mes.integration.sync;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SyncJobInitializer {
    @Bean
    public CommandLineRunner seedSyncJobs(SyncJobService service) {
        return args -> service.ensureDefaults();
    }
}
