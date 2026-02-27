package dev.gmky.utils.csv.reader;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.exception.CsvParsingException;
import dev.gmky.utils.csv.mapper.AnnotationCsvRowMapper;
import dev.gmky.utils.csv.mapper.CsvRowMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

import java.io.InputStreamReader;

/**
 * Spring Batch {@code ItemReader} that reads and maps CSV rows to DTOs using
 * {@link AnnotationCsvRowMapper} backed by OpenCSV.
 * <p>
 * Respects the Spring Batch {@code open/read/close} lifecycle and integrates
 * with {@link dev.gmky.utils.batch.config.BatchJobFactory#createStep}.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * CsvBatchReader<UserDto> reader = new CsvBatchReader<>(UserDto.class, resource);
 * Step step = batchJobFactory.createStep("importUsers", reader, processor, writer, batchConfig);
 * }</pre>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
@Slf4j
public class CsvBatchReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

    @NonNull private final Class<T> targetType;
    @NonNull private final Resource resource;
    @NonNull private final CsvReaderConfig config;

    private CSVReader csvReader;
    private CsvRowMapper<T> mapper;
    private String[] headers;
    private long lineNumber;

    /**
     * Creates a reader with default config (reads from {@code @CsvRecord} annotation or defaults).
     */
    public CsvBatchReader(Class<T> targetType, Resource resource) {
        this(targetType, resource, OpenCsvStreamingReader.resolveConfigPublic(targetType));
    }

    /**
     * Creates a reader with explicit config.
     */
    public CsvBatchReader(Class<T> targetType, Resource resource, CsvReaderConfig config) {
        this.targetType = targetType;
        this.resource = resource;
        this.config = config;
        setName(ClassUtils.getShortName(getClass()) + "[" + targetType.getSimpleName() + "]");
    }

    @Override
    protected void doOpen() throws Exception {
        log.debug("Opening CsvBatchReader for type {} on resource {}",
                targetType.getSimpleName(), resource.getFilename());
        var parser = new CSVParserBuilder()
                .withSeparator(config.getDelimiter())
                .withQuoteChar(config.getQuoteChar())
                .withEscapeChar(config.getEscapeChar())
                .withIgnoreLeadingWhiteSpace(config.isTrimValues())
                .build();
        this.csvReader = new CSVReaderBuilder(
                new InputStreamReader(resource.getInputStream(), config.getCharset()))
                .withCSVParser(parser)
                .build();

        if (config.isHasHeader()) {
            this.headers = csvReader.readNext();
            log.debug("CSV headers: {}", (Object) headers);
        }
        this.mapper = new AnnotationCsvRowMapper<>(targetType, headers);
        this.lineNumber = config.isHasHeader() ? 1 : 0;
    }

    @Override
    protected T doRead() throws Exception {
        String[] row;
        while ((row = csvReader.readNext()) != null) {
            lineNumber++;
            // Skip empty rows
            if (row.length == 0 || (row.length == 1 && row[0].isBlank())) continue;
            try {
                return mapper.map(row, headers, lineNumber);
            } catch (Exception e) {
                log.warn("CsvBatchReader: row {} mapping failed: {}", lineNumber, e.getMessage());
                throw new CsvParsingException(lineNumber, String.join(",", row), e.getMessage(), e);
            }
        }
        return null; // signals end-of-file to Spring Batch
    }

    @Override
    protected void doClose() throws Exception {
        if (csvReader != null) {
            csvReader.close();
            log.debug("CsvBatchReader closed. Total rows processed: {}", lineNumber);
        }
    }
}
