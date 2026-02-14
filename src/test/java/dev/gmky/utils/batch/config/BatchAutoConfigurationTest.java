package dev.gmky.utils.batch.config;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for BatchAutoConfiguration.
 */
class BatchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class));

    @Test
    void testAutoConfigurationEnabled() {
        contextRunner
            .withUserConfiguration(BatchTestConfig.class)
            .run(context -> {
                assertThat(context).hasSingleBean(BatchJobFactory.class);
                assertThat(context).hasSingleBean(LoggingJobExecutionListener.class);
                assertThat(context).hasSingleBean(LoggingStepExecutionListener.class);
            });
    }

    @Test
    void testAutoConfigurationDisabledWhenMissingDependencies() {
        // Test behavior when dependencies are missing if needed
    }

    @Configuration
    @EnableBatchProcessing
    static class BatchTestConfig {
        @Bean
        public JobRepository jobRepository() {
            return mock(JobRepository.class);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        public DataSource dataSource() {
            return mock(DataSource.class);
        }
    }
}
