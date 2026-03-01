package dev.gmky.utils.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Factory for creating batch jobs with consistent configuration.
 * <p>
 * Provides methods for creating simple and multi-step jobs.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@RequiredArgsConstructor
public class BatchJobFactory {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    /**
     * Creates a simple one-step job.
     *
     * @param jobName the name of the job
     * @param reader the item reader
     * @param processor the item processor (can be null)
     * @param writer the item writer
     * @param config the job configuration
     * @param <I> input type
     * @param <O> output type
     * @return the configured Job
     */
    public <I, O> Job createSimpleJob(
            String jobName,
            ItemReader<I> reader,
            ItemProcessor<I, O> processor,
            ItemWriter<O> writer,
            BatchJobConfig config) {

        Step step = createStep(jobName + "Step", reader, processor, writer, config);

        return new JobBuilder(jobName, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

    /**
     * Creates a step with standard configuration.
     *
     * @param stepName the name of the step
     * @param reader the item reader
     * @param processor the item processor (can be null)
     * @param writer the item writer
     * @param config the job configuration
     * @param <I> input type
     * @param <O> output type
     * @return the configured Step
     */
    public <I, O> Step createStep(
            String stepName,
            ItemReader<I> reader,
            ItemProcessor<I, O> processor,
            ItemWriter<O> writer,
            BatchJobConfig config) {

        StepBuilder stepBuilder = new StepBuilder(stepName, jobRepository);

        var chunkBuilder = stepBuilder.<I, O>chunk(config.getChunkSize(), transactionManager)
                .reader(reader)
                .writer(writer);

        // Processor is optional
        if (processor != null) {
            chunkBuilder.processor(processor);
        }

        // Configure fault tolerance if skip/retry limits are set
        if (config.getSkipLimit() > 0 || config.getRetryLimit() > 0) {
            var faultTolerantBuilder = chunkBuilder.faultTolerant();

            if (config.getSkipLimit() > 0) {
                faultTolerantBuilder.skipLimit(config.getSkipLimit());
                config.getSkippableExceptions().forEach(faultTolerantBuilder::skip);
            }

            if (config.getRetryLimit() > 0) {
                faultTolerantBuilder.retryLimit(config.getRetryLimit());
                config.getRetryableExceptions().forEach(faultTolerantBuilder::retry);
            }

            return faultTolerantBuilder.build();
        }

        return chunkBuilder.build();
    }

    /**
     * Creates a multistep job with sequential execution.
     *
     * @param jobName the name of the job
     * @param steps the list of steps to execute
     * @return the configured Job
     */
    public Job createMultiStepJob(String jobName, List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Steps list cannot be null or empty");
        }

        var jobBuilder = new JobBuilder(jobName, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(steps.getFirst());

        // Chain remaining steps
        for (int i = 1; i < steps.size(); i++) {
            jobBuilder.next(steps.get(i));
        }

        return jobBuilder.build();
    }

    /**
     * Get job repository for custom job builders.
     *
     * @param jobName the name of the job
     * @return a new JobBuilder
     */
    public JobBuilder createJobBuilder(String jobName) {
        return new JobBuilder(jobName, jobRepository)
                .incrementer(new RunIdIncrementer());
    }

    /**
     * Get the job repository.
     *
     * @return the job repository
     */
    public JobRepository getJobRepository() {
        return jobRepository;
    }

    /**
     * Get the transaction manager.
     *
     * @return the transaction manager
     */
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
}
