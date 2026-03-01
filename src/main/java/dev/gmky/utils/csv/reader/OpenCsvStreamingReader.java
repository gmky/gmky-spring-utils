package dev.gmky.utils.csv.reader;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import dev.gmky.utils.csv.annotation.CsvRecord;
import dev.gmky.utils.csv.callback.CsvReadCallback;
import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.config.CsvReaderConfig.ErrorStrategy;
import dev.gmky.utils.csv.exception.CsvParsingException;
import dev.gmky.utils.csv.mapper.AnnotationCsvRowMapper;
import dev.gmky.utils.csv.mapper.CsvRowMapper;
import dev.gmky.utils.csv.model.CsvError;
import dev.gmky.utils.csv.model.CsvReadResult;
import dev.gmky.utils.csv.validator.CsvRowValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default {@link dev.gmky.utils.csv.reader.CsvReader} implementation backed by OpenCSV.
 * <p>
 * OpenCSV handles RFC 4180-compliant tokenizing (multiline quoted fields, BOM detection,
 * escaped quotes, configurable delimiters). This class layers the annotation-driven DTO
 * mapping, error handling, lifecycle callbacks, and validator on top.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * OpenCsvStreamingReader<UserDto> reader = OpenCsvStreamingReader.forType(UserDto.class);
 * List<UserDto> users = reader.readAll(new FileInputStream("users.csv"));
 * }</pre>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
@Slf4j
public class OpenCsvStreamingReader<T> implements dev.gmky.utils.csv.reader.CsvReader<T> {

    private final Class<T> targetType;
    private final CsvReaderConfig config;
    private final CsvRowMapper<T> customMapper; // null = use AnnotationCsvRowMapper

    private OpenCsvStreamingReader(Class<T> targetType, CsvReaderConfig config, CsvRowMapper<T> customMapper) {
        this.targetType = targetType;
        this.config = config;
        this.customMapper = customMapper;
    }

    // ----------------------------- Factory methods -----------------------------

    /** Creates a reader using annotation-driven mapping and default config. */
    public static <T> OpenCsvStreamingReader<T> forType(Class<T> targetType) {
        return forType(targetType, resolveConfig(targetType));
    }

    /** Creates a reader with explicit config. Annotation config on the DTO is overridden. */
    public static <T> OpenCsvStreamingReader<T> forType(Class<T> targetType, CsvReaderConfig config) {
        return new OpenCsvStreamingReader<>(targetType, config, null);
    }

    /** Creates a reader with a fully custom row mapper (annotations are bypassed). */
    public static <T> OpenCsvStreamingReader<T> withMapper(
            Class<T> targetType, CsvReaderConfig config, CsvRowMapper<T> mapper) {
        return new OpenCsvStreamingReader<>(targetType, config, mapper);
    }

    // ---------------------------- Public API -----------------------------------

    @Override
    public List<T> readAll(InputStream inputStream) {
        List<T> results = new ArrayList<>();
        read(inputStream, results::add);
        return results;
    }

    @Override
    public Stream<T> stream(InputStream inputStream) {
        try {
            var csvReader = buildCsvReader(inputStream);
            String[] headers = config.isHasHeader() ? csvReader.readNext() : null;
            fireOnHeader(headers);

            var mapper = resolveMapper(headers);
            java.util.concurrent.atomic.AtomicLong lineCounter = new java.util.concurrent.atomic.AtomicLong(config.isHasHeader() ? 1 : 0);

            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(csvReader.iterator(), java.util.Spliterator.ORDERED),
                    config.isParallel()
            ).map(columns -> {
                long currentLine = lineCounter.incrementAndGet();
                if (shouldSkipEmpty(columns)) return null;
                try {
                    String[] processedColumns = trimIfNeeded(columns);
                    return mapper.map(processedColumns, headers, currentLine);
                } catch (Exception e) {
                    log.warn("Row {} mapping failed: {}", currentLine, e.getMessage());
                    return null;
                }
            }).filter(Objects::nonNull)
              .onClose(() -> {
                  try {
                      csvReader.close();
                  } catch (IOException ignored) {
                      log.debug("Failed to close CSVReader in stream onClose handler");
                  }
              });
        } catch (Exception e) {
            throw new CsvParsingException(0, "", "Failed to open/read CSV stream", e);
        }
    }

    @Override
    public void read(InputStream inputStream, Consumer<T> rowConsumer) {
        readWithResult(inputStream, rowConsumer);
    }

    @Override
    public CsvReadResult<T> readWithResult(InputStream inputStream) {
        return readWithResult(inputStream, null);
    }

    // ---------------------------- Internal logic -----------------------------------

    @SuppressWarnings("unchecked")
    private CsvReadResult<T> readWithResult(InputStream inputStream, Consumer<T> externalConsumer) {
        Instant start = Instant.now();
        List<T> successRecords = new ArrayList<>();
        List<CsvError> errors = new ArrayList<>();
        long[] totalRows = {0};
        String[] headers = null;

        fireOnStart();

        try (CSVReader csvReader = buildCsvReader(inputStream)) {
            if (config.isHasHeader()) {
                headers = csvReader.readNext();
                fireOnHeader(headers);
            }

            CsvRowMapper<T> mapper = resolveMapper(headers);
            CsvRowValidator<T> validator = (CsvRowValidator<T>) config.getValidator();
            long lineNumber = config.isHasHeader() ? 1 : 0;
            String[] row;

            while ((row = csvReader.readNext()) != null) {
                lineNumber++;
                totalRows[0]++;

                if (shouldSkipEmpty(row)) continue;

                // Check max-errors guard
                if (config.getMaxErrors() >= 0 && errors.size() >= config.getMaxErrors()) {
                    log.warn("Max errors ({}) reached. Stopping early at line {}.",
                            config.getMaxErrors(), lineNumber);
                    break;
                }

                String rawLine = String.join(String.valueOf(config.getDelimiter()), row);
                String[] processed = trimIfNeeded(row);

                try {
                    T record = mapper.map(processed, headers, lineNumber);

                    // Validate
                    if (validator != null) {
                        List<String> violations = validator.validate(record);
                        if (!violations.isEmpty()) {
                            handleError(errors, lineNumber, rawLine, null,
                                    new dev.gmky.utils.csv.exception.CsvValidationException(lineNumber, violations));
                            continue;
                        }
                    }

                    successRecords.add(record);
                    if (externalConsumer != null) externalConsumer.accept(record);
                    fireOnRow(lineNumber, record);

                } catch (dev.gmky.utils.csv.exception.CsvParsingException e) {
                    // Already handled and thrown by a nested call (e.g., handleError in FAIL_FAST)
                    throw e;
                } catch (Exception e) {
                    handleError(errors, lineNumber, rawLine, e.getMessage(), e);
                }
            }

        } catch (dev.gmky.utils.csv.exception.CsvParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvParsingException(0, "", "Failed to read CSV", e);
        }

        Duration elapsed = Duration.between(start, Instant.now());
        CsvReadResult<T> result = new CsvReadResult<>(
                successRecords, errors, totalRows[0],
                successRecords.size(), errors.size(), elapsed
        );
        fireOnComplete(result);
        return result;
    }

    private void handleError(List<CsvError> errors, long line, String raw, String msg, Exception e) {
        ErrorStrategy strategy = config.getErrorStrategy();
        if (strategy == ErrorStrategy.FAIL_FAST) {
            throw new CsvParsingException(line, raw, msg != null ? msg : e.getMessage(), e);
        }
        fireOnError(line, raw, e);
        if (strategy == ErrorStrategy.SKIP_AND_LOG) {
            errors.add(CsvError.of(line, raw, e.getMessage(), e));
        }
        // SKIP_SILENT: do nothing
    }

    private CsvRowMapper<T> resolveMapper(String[] headers) {
        if (customMapper != null) return customMapper;
        return new AnnotationCsvRowMapper<>(targetType, headers);
    }

    private CSVReader buildCsvReader(InputStream inputStream) {
        var parser = new CSVParserBuilder()
                .withSeparator(config.getDelimiter())
                .withQuoteChar(config.getQuoteChar())
                .withEscapeChar(config.getEscapeChar())
                .withIgnoreLeadingWhiteSpace(config.isTrimValues())
                .build();
        return new CSVReaderBuilder(new InputStreamReader(inputStream, config.getCharset()))
                .withCSVParser(parser)
                .withSkipLines(0)
                .build();
    }

    private boolean shouldSkipEmpty(String[] row) {
        return config.isSkipEmptyLines() && (row == null || row.length == 0
                || (row.length == 1 && StringUtils.isBlank(row[0])));
    }

    private String[] trimIfNeeded(String[] columns) {
        if (!config.isTrimValues()) return columns;
        String[] trimmed = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            trimmed[i] = columns[i] != null ? columns[i].trim() : null;
        }
        return trimmed;
    }

    // ---------------------- Lifecycle helpers ----------------------------------

    @SuppressWarnings("unchecked")
    private void fireOnStart() {
        CsvReadCallback<T> cb = (CsvReadCallback<T>) config.getCallback();
        if (cb != null) cb.onStart(config);
    }

    @SuppressWarnings("unchecked")
    private void fireOnHeader(String[] headers) {
        CsvReadCallback<T> cb = (CsvReadCallback<T>) config.getCallback();
        if (cb != null && headers != null) cb.onHeader(headers);
    }

    @SuppressWarnings("unchecked")
    private void fireOnRow(long lineNumber, T record) {
        CsvReadCallback<T> cb = (CsvReadCallback<T>) config.getCallback();
        if (cb != null) cb.onRow(lineNumber, record);
    }

    @SuppressWarnings("unchecked")
    private void fireOnError(long lineNumber, String rawLine, Exception e) {
        CsvReadCallback<T> cb = (CsvReadCallback<T>) config.getCallback();
        if (cb != null) cb.onError(lineNumber, rawLine, e);
    }

    @SuppressWarnings("unchecked")
    private void fireOnComplete(CsvReadResult<T> result) {
        CsvReadCallback<T> cb = (CsvReadCallback<T>) config.getCallback();
        if (cb != null) cb.onComplete(result);
    }

    /** Reads config from @CsvRecord annotation on the target DTO, or returns defaults. */
    public static CsvReaderConfig resolveConfigPublic(Class<?> targetType) {
        return resolveConfig(targetType);
    }

    /** Reads config from @CsvRecord annotation on the target DTO, or returns defaults. */
    private static CsvReaderConfig resolveConfig(Class<?> targetType) {
        CsvRecord annotation = targetType.getAnnotation(CsvRecord.class);
        if (annotation == null) return CsvReaderConfig.defaultConfig();
        return CsvReaderConfig.builder()
                .delimiter(annotation.delimiter())
                .charset(java.nio.charset.Charset.forName(annotation.encoding()))
                .hasHeader(annotation.hasHeader())
                .skipEmptyLines(annotation.skipEmptyLines())
                .quoteChar(annotation.quoteChar())
                .escapeChar(annotation.escapeChar())
                .build();
    }
}
