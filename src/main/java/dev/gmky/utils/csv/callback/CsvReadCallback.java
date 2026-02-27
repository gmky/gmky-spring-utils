package dev.gmky.utils.csv.callback;

import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.model.CsvReadResult;

/**
 * Lifecycle callback interface for CSV read operations.
 * <p>
 * All methods have default no-op implementations so you only override what you need.
 * </p>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
public interface CsvReadCallback<T> {

    /** Called when reading begins, before the first row is processed. */
    default void onStart(CsvReaderConfig config) {}

    /** Called immediately after the header row is read (if hasHeader is true). */
    default void onHeader(String[] headers) {}

    /** Called after each row is successfully mapped to a DTO. */
    default void onRow(long lineNumber, T record) {}

    /** Called when a row fails to parse or map. */
    default void onError(long lineNumber, String rawLine, Exception e) {}

    /** Called after all rows have been processed. */
    default void onComplete(CsvReadResult<T> result) {}
}
