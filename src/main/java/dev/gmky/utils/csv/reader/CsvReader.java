package dev.gmky.utils.csv.reader;

import dev.gmky.utils.csv.model.CsvReadResult;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Main public API for reading CSV files into DTOs.
 * <p>
 * Provides multiple consumption patterns:
 * <ul>
 *   <li>Eager: {@link #readAll} loads all rows into a {@code List}</li>
 *   <li>Lazy: {@link #stream} returns a {@code Stream<T>} for large files</li>
 *   <li>Callback: {@link #read} processes rows without materializing them</li>
 *   <li>Error-aware: {@link #readWithResult} captures errors per-row</li>
 * </ul>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
public interface CsvReader<T> {

    /**
     * Reads all rows eagerly into a {@link List}.
     * <p>Suitable for small-to-medium files where all data is needed upfront.</p>
     *
     * @param inputStream the CSV input stream
     * @return list of successfully mapped DTOs
     */
    List<T> readAll(InputStream inputStream);

    /**
     * Returns a lazy {@link Stream} of DTOs.
     * <p>Suitable for large files. The stream must be closed after use (use try-with-resources).</p>
     *
     * @param inputStream the CSV input stream
     * @return lazy stream of mapped DTOs
     */
    Stream<T> stream(InputStream inputStream);

    /**
     * Processes each row via the provided {@link Consumer} without materializing the result.
     * <p>Lowest memory usage mode.</p>
     *
     * @param inputStream the CSV input stream
     * @param rowConsumer the consumer to invoke for each mapped DTO
     */
    void read(InputStream inputStream, Consumer<T> rowConsumer);

    /**
     * Reads all rows and collects both successes and per-row errors into a {@link CsvReadResult}.
     *
     * @param inputStream the CSV input stream
     * @return a result object containing success records, errors, and statistics
     */
    CsvReadResult<T> readWithResult(InputStream inputStream);
}
