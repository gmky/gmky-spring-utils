package dev.gmky.utils.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for BatchJobFactory.
 */
@ExtendWith(MockitoExtension.class)
class BatchJobFactoryTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ItemReader<String> reader;

    @Mock
    private ItemProcessor<String, String> processor;

    @Mock
    private ItemWriter<String> writer;

    @InjectMocks
    private BatchJobFactory factory;

    @BeforeEach
    void setUp() {
        // Mocks are injected automatically
    }

    @Test
    void testCreateSimpleJob() {
        BatchJobConfig config = BatchJobConfig.simple(100);

        Job job = factory.createSimpleJob(
            "testJob",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(job);
        assertEquals("testJob", job.getName());
    }

    @Test
    void testCreateSimpleJobWithoutProcessor() {
        BatchJobConfig config = BatchJobConfig.simple(100);

        Job job = factory.createSimpleJob(
            "testJob",
            reader,
            null, // No processor
            writer,
            config
        );

        assertNotNull(job);
        assertEquals("testJob", job.getName());
    }

    @Test
    void testCreateStepWithProcessor() {
        BatchJobConfig config = BatchJobConfig.simple(50);

        Step step = factory.createStep(
            "testStep",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(step);
        assertEquals("testStep", step.getName());
    }

    @Test
    void testCreateStepWithoutProcessor() {
        BatchJobConfig config = BatchJobConfig.simple(50);

        Step step = factory.createStep(
            "testStep",
            reader,
            null,
            writer,
            config
        );

        assertNotNull(step);
        assertEquals("testStep", step.getName());
    }

    @Test
    void testCreateStepWithFaultTolerance() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(100)
            .skipLimit(10)
            .retryLimit(3)
            .skippableExceptions(Collections.singletonList(IllegalArgumentException.class))
            .retryableExceptions(Collections.singletonList(RuntimeException.class))
            .build();

        Step step = factory.createStep(
            "faultTolerantStep",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(step);
        assertEquals("faultTolerantStep", step.getName());
    }

    @Test
    void testCreateStepWithoutFaultTolerance() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(100)
            .skipLimit(0)
            .retryLimit(0)
            .build();

        Step step = factory.createStep(
            "simpleStep",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(step);
        assertEquals("simpleStep", step.getName());
    }

    @Test
    void testCreateStepWithSkipLimitOnlyNoRetry() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(100)
            .skipLimit(10)
            .retryLimit(0)
            .skippableExceptions(Collections.singletonList(IllegalArgumentException.class))
            .build();

        Step step = factory.createStep(
            "skipStep",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(step);
        assertEquals("skipStep", step.getName());
    }

    @Test
    void testCreateStepWithRetryLimitOnlyNoSkip() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(100)
            .skipLimit(0)
            .retryLimit(3)
            .retryableExceptions(Collections.singletonList(RuntimeException.class))
            .build();

        Step step = factory.createStep(
            "retryStep",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(step);
        assertEquals("retryStep", step.getName());
    }

    @Test
    void testCreateMultiStepJob() {
        Step step1 = mock(Step.class);
        Step step2 = mock(Step.class);
        Step step3 = mock(Step.class);

        // Removed unnecessary stubs for getName() as they are not called by the factory method

        List<Step> steps = Arrays.asList(step1, step2, step3);

        Job job = factory.createMultiStepJob("multiStepJob", steps);

        assertNotNull(job);
        assertEquals("multiStepJob", job.getName());
    }

    @Test
    void testCreateMultiStepJobWithSingleStep() {
        Step step = mock(Step.class);
        // Removed unnecessary stub

        Job job = factory.createMultiStepJob("singleStepJob", Collections.singletonList(step));

        assertNotNull(job);
        assertEquals("singleStepJob", job.getName());
    }

    @Test
    void testCreateMultiStepJobWithNullSteps() {
        assertThrows(IllegalArgumentException.class, () ->
            factory.createMultiStepJob("job", null)
        );
    }

    @Test
    void testCreateMultiStepJobWithEmptySteps() {
        assertThrows(IllegalArgumentException.class, () ->
            factory.createMultiStepJob("job", Collections.emptyList())
        );
    }

    @Test
    void testCreateJobBuilder() {
        var jobBuilder = factory.createJobBuilder("customJob");

        assertNotNull(jobBuilder);
    }

    @Test
    void testGetJobRepository() {
        JobRepository repo = factory.getJobRepository();

        assertNotNull(repo);
        assertEquals(jobRepository, repo);
    }

    @Test
    void testGetTransactionManager() {
        PlatformTransactionManager txManager = factory.getTransactionManager();

        assertNotNull(txManager);
        assertEquals(transactionManager, txManager);
    }

    @Test
    void testCreateJobWithCustomChunkSize() {
        BatchJobConfig config = BatchJobConfig.simple(500);

        Job job = factory.createSimpleJob(
            "largeChunkJob",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(job);
    }

    @Test
    void testCreateJobWithHighSkipLimit() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(100)
            .skipLimit(1000)
            .skippableExceptions(java.util.List.of(RuntimeException.class))
            .build();

        Job job = factory.createSimpleJob(
            "highSkipJob",
            reader,
            processor,
            writer,
            config
        );

        assertNotNull(job);
    }
}
