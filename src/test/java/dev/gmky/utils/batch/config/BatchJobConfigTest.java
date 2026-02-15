package dev.gmky.utils.batch.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BatchJobConfig.
 */
class BatchJobConfigTest {

    @Test
    void testDefaultConfig() {
        BatchJobConfig config = BatchJobConfig.defaultConfig();

        assertEquals(100, config.getChunkSize());
        assertEquals(10, config.getSkipLimit());
        assertEquals(3, config.getRetryLimit());
        assertNotNull(config.getSkippableExceptions());
        assertNotNull(config.getRetryableExceptions());
    }

    @Test
    void testSimpleConfig() {
        BatchJobConfig config = BatchJobConfig.simple(500);

        assertEquals(500, config.getChunkSize());
        assertEquals(0, config.getSkipLimit());
        assertEquals(0, config.getRetryLimit());
    }

    @Test
    void testBuilderWithAllParameters() {
        List<Class<? extends Exception>> skippable = Arrays.asList(
            IllegalArgumentException.class,
            NullPointerException.class
        );
        List<Class<? extends Exception>> retryable = Arrays.asList(
            RuntimeException.class
        );

        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(200)
            .skipLimit(20)
            .retryLimit(5)
            .skippableExceptions(skippable)
            .retryableExceptions(retryable)
            .build();

        assertEquals(200, config.getChunkSize());
        assertEquals(20, config.getSkipLimit());
        assertEquals(5, config.getRetryLimit());
        assertEquals(skippable, config.getSkippableExceptions());
        assertEquals(retryable, config.getRetryableExceptions());
    }

    @Test
    void testBuilderWithPartialParameters() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(300)
            .build();

        assertEquals(300, config.getChunkSize());
        assertEquals(10, config.getSkipLimit()); // Default
        assertEquals(3, config.getRetryLimit()); // Default
    }

    @Test
    void testSettersAndGetters() {
        BatchJobConfig config = BatchJobConfig.builder().build();
        config.setChunkSize(150);
        config.setSkipLimit(15);
        config.setRetryLimit(4);

        assertEquals(150, config.getChunkSize());
        assertEquals(15, config.getSkipLimit());
        assertEquals(4, config.getRetryLimit());
    }

    @Test
    void testCustomExceptions() {
        List<Class<? extends Exception>> customSkippable = Arrays.asList(
            CustomException.class
        );

        BatchJobConfig config = BatchJobConfig.builder()
            .skippableExceptions(customSkippable)
            .build();

        assertTrue(config.getSkippableExceptions().contains(CustomException.class));
    }

    @Test
    void testNoFaultTolerance() {
        BatchJobConfig config = BatchJobConfig.simple(100);

        assertEquals(0, config.getSkipLimit());
        assertEquals(0, config.getRetryLimit());
    }

    @Test
    void testHighThroughputConfig() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(1000)
            .skipLimit(0)
            .retryLimit(0)
            .build();

        assertEquals(1000, config.getChunkSize());
        assertEquals(0, config.getSkipLimit());
        assertEquals(0, config.getRetryLimit());
    }

    @Test
    void testFaultTolerantConfig() {
        BatchJobConfig config = BatchJobConfig.builder()
            .chunkSize(50)
            .skipLimit(100)
            .retryLimit(10)
            .build();

        assertEquals(50, config.getChunkSize());
        assertEquals(100, config.getSkipLimit());
        assertEquals(10, config.getRetryLimit());
    }

    /**
     * Custom exception for testing.
     */
    static class CustomException extends Exception {
        public CustomException(String message) {
            super(message);
        }
    }
}
