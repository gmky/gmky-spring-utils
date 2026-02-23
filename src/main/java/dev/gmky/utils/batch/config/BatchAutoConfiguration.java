package dev.gmky.utils.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Batch utilities.
 * <p>
 * Activated when Spring Batch is on the classpath.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@AutoConfiguration
@ConditionalOnClass(JobRepository.class)
@EnableBatchProcessing
public class BatchAutoConfiguration {

    /**
     * Create BatchJobFactory bean if not already defined.
     *
     * @return the batch job factory
     */
    @Bean
    @ConditionalOnMissingBean
    public BatchJobFactory batchJobFactory(JobRepository jobRepository, org.springframework.transaction.PlatformTransactionManager transactionManager) {
        return new BatchJobFactory(jobRepository, transactionManager);
    }

    /**
     * Create logging job execution listener.
     *
     * @return the logging job execution listener
     */
    @Bean
    @ConditionalOnMissingBean
    public LoggingJobExecutionListener loggingJobExecutionListener() {
        return new LoggingJobExecutionListener();
    }

    /**
     * Create logging step execution listener.
     *
     * @return the logging step execution listener
     */
    @Bean
    @ConditionalOnMissingBean
    public LoggingStepExecutionListener loggingStepExecutionListener() {
        return new LoggingStepExecutionListener();
    }
}
