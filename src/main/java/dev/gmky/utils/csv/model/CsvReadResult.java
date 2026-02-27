package dev.gmky.utils.csv.model;

import java.time.Duration;
import java.util.List;

/**
 * Contains the full result of a CSV read operation with per-row error tracking.
 * <p>
 * Returned by {@link dev.gmky.utils.csv.reader.CsvReader#readWithResult}.
 * </p>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
public record CsvReadResult<T>(
    List<T> successRecords,
    List<CsvError> errors,
    long totalRows,
    long successCount,
    long errorCount,
    Duration elapsed
) {
    /**
     * Returns true if no mapping errors occurred.
     */
    public boolean isFullySuccessful() {
        return errors.isEmpty();
    }
}
