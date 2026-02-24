package dev.gmky.utils.batch.config;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration object for batch jobs.
 * <p>
 * Contains common settings like chunk size, skip/retry limits, and exception handling.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Data
@Builder
public class BatchJobConfig {

    /**
     * Number of items to process in each chunk.
     */
    @Builder.Default
    private Integer chunkSize = 100;

    /**
     * Maximum number of items to skip on errors.
     */
    @Builder.Default
    private Integer skipLimit = 10;

    /**
     * Maximum number of retry attempts for failed items.
     */
    @Builder.Default
    private Integer retryLimit = 3;

    /**
     * Exceptions that should cause items to be skipped.
     */
    @Builder.Default
    private List<Class<? extends Exception>> skippableExceptions = new ArrayList<>(
            Collections.singletonList(RuntimeException.class)
    );

    /**
     * Exceptions that should trigger retry attempts.
     */
    @Builder.Default
    private List<Class<? extends Exception>> retryableExceptions = new ArrayList<>(
            Collections.singletonList(RuntimeException.class)
    );

    /**
     * Create a configuration with default values.
     *
     * @return a new BatchJobConfig with defaults
     */
    public static BatchJobConfig defaultConfig() {
        return BatchJobConfig.builder().build();
    }

    /**
     * Create a simple configuration with just chunk size.
     *
     * @param chunkSize the chunk size
     * @return a new BatchJobConfig
     */
    public static BatchJobConfig simple(int chunkSize) {
        return BatchJobConfig.builder()
            .chunkSize(chunkSize)
            .skipLimit(0) // No skipping by default
            .retryLimit(0) // No retries by default
            .build();
    }
}
