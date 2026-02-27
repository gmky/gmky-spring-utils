package dev.gmky.utils.csv.callback;

import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.model.CsvReadResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Default logging implementation of {@link CsvReadCallback}.
 * Logs key lifecycle events at DEBUG and INFO level using SLF4J.
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
@Slf4j
public class LoggingCsvReadCallback<T> implements CsvReadCallback<T> {

    @Override
    public void onStart(CsvReaderConfig config) {
        log.debug("CSV reading started. Delimiter='{}', hasHeader={}, charset={}",
                config.getDelimiter(), config.isHasHeader(), config.getCharset());
    }

    @Override
    public void onHeader(String[] headers) {
        log.debug("CSV headers detected: {}", (Object) headers);
    }

    @Override
    public void onRow(long lineNumber, T record) {
        log.trace("CSV row {} mapped successfully", lineNumber);
    }

    @Override
    public void onError(long lineNumber, String rawLine, Exception e) {
        log.warn("CSV row {} failed to map: {}. Raw=[{}]", lineNumber, e.getMessage(), rawLine);
    }

    @Override
    public void onComplete(CsvReadResult<T> result) {
        log.info("CSV reading complete: {} total rows, {} success, {} errors, elapsed={}ms",
                result.totalRows(), result.successCount(), result.errorCount(),
                result.elapsed().toMillis());
    }
}
