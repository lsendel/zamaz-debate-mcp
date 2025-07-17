package com.zamaz.mcp.common.backup;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for backup and disaster recovery components
 */
@Configuration
@EnableAsync
@EnableScheduling
public class BackupConfiguration {
    
    /**
     * Configure executor for backup operations
     */
    @Bean(name = "backupExecutor")
    @ConditionalOnMissingBean(name = "backupExecutor")
    public Executor backupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Backup-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * Configure backup health indicator
     */
    @Bean
    @ConditionalOnMissingBean
    public BackupHealthIndicator backupHealthIndicator(BackupRepository backupRepository) {
        return new BackupHealthIndicator(backupRepository);
    }
    
    /**
     * Configure backup metrics
     */
    @Bean
    @ConditionalOnMissingBean
    public BackupMetrics backupMetrics(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        return new BackupMetrics(meterRegistry);
    }
}