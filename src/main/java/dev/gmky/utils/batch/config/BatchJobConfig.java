package dev.gmky.utils.batch.config;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
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
    private int chunkSize = 100;

    /**
     * Maximum number of items to skip on errors.
     * Set to 0 to disable skipping (default).
     */
    @Builder.Default
    private int skipLimit = 0;

    /**
     * Maximum number of retry attempts for failed items.
     * Set to 0 to disable retries (default).
     */
    @Builder.Default
    private int retryLimit = 0;

    /**
     * Exceptions that should cause items to be skipped.
     * Empty by default — callers must explicitly add exception types to enable skip behavior.
     */
    @Builder.Default
    private List<Class<? extends Exception>> skippableExceptions = new ArrayList<>();

    /**
     * Exceptions that should trigger retry attempts.
     * Empty by default — callers must explicitly add exception types to enable retry behavior.
     */
    @Builder.Default
    private List<Class<? extends Exception>> retryableExceptions = new ArrayList<>();

    /**
     * Create a configuration with default values (no skip, no retry).
     *
     * @return a new BatchJobConfig with defaults
     */
    public static BatchJobConfig defaultConfig() {
        return BatchJobConfig.builder().build();
    }

    /**
     * Create a simple configuration with just chunk size (no skip, no retry).
     *
     * @param chunkSize the chunk size
     * @return a new BatchJobConfig
     */
    public static BatchJobConfig simple(int chunkSize) {
        return BatchJobConfig.builder()
            .chunkSize(chunkSize)
            .build();
    }
}
