package dev.gmky.utils.csv.config;

import dev.gmky.utils.csv.callback.CsvReadCallback;
import dev.gmky.utils.csv.validator.CsvRowValidator;
import lombok.Builder;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for the CSV reader.
 * <p>
 * Use the {@link CsvReaderConfigBuilder builder} to construct an instance
 * with custom settings. All settings have sensible defaults.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * CsvReaderConfig config = CsvReaderConfig.builder()
 *     .delimiter(';')
 *     .charset(StandardCharsets.ISO_8859_1)
 *     .errorStrategy(ErrorStrategy.SKIP_AND_LOG)
 *     .maxErrors(50)
 *     .build();
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Getter
@Builder
public class CsvReaderConfig {

    /**
     * Column delimiter character. Defaults to comma {@code ','}.
     */
    @Builder.Default
    private final char delimiter = ',';

    /**
     * File charset. Defaults to UTF-8.
     */
    @Builder.Default
    private final Charset charset = StandardCharsets.UTF_8;

    /**
     * Whether the first row is a header row. Defaults to true.
     */
    @Builder.Default
    private final boolean hasHeader = true;

    /**
     * Whether to skip blank lines. Defaults to true.
     */
    @Builder.Default
    private final boolean skipEmptyLines = true;

    /**
     * Quote character for enclosed fields. Defaults to double-quote {@code '"'}.
     */
    @Builder.Default
    private final char quoteChar = '"';

    /**
     * Escape character. Defaults to backslash {@code '\\'}.
     */
    @Builder.Default
    private final char escapeChar = '\\';

    /**
     * Whether to trim whitespace from cell values. Defaults to true.
     */
    @Builder.Default
    private final boolean trimValues = true;

    /**
     * Error handling strategy. Defaults to {@link ErrorStrategy#SKIP_AND_LOG}.
     */
    @Builder.Default
    private final ErrorStrategy errorStrategy = ErrorStrategy.SKIP_AND_LOG;

    /**
     * Maximum number of errors to tolerate. After this, reading stops even in SKIP modes.
     * Set to -1 for unlimited. Defaults to 100.
     */
    @Builder.Default
    private final int maxErrors = 100;

    /**
     * Whether to enable parallel DTO mapping for large files. Defaults to false.
     */
    @Builder.Default
    private final boolean parallel = false;

    /**
     * Batch size for parallel mapping. Defaults to 5000.
     */
    @Builder.Default
    private final int batchSize = 5000;

    /**
     * Optional row validator applied after mapping. Null means no validation.
     */
    @Builder.Default
    private final CsvRowValidator<?> validator = null;

    /**
     * Optional lifecycle callback. Null means no callbacks.
     */
    @Builder.Default
    private final CsvReadCallback<?> callback = null;

    /**
     * Returns a default configuration instance.
     */
    public static CsvReaderConfig defaultConfig() {
        return CsvReaderConfig.builder().build();
    }

    /**
     * Error handling strategies for rows that fail to parse or map.
     */
    public enum ErrorStrategy {
        /** Throw an exception on the first error encountered. */
        FAIL_FAST,
        /** Skip bad rows and collect errors in the result. */
        SKIP_AND_LOG,
        /** Skip bad rows silently without collecting errors. */
        SKIP_SILENT
    }
}
