package dev.gmky.utils.csv.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as mappable from a CSV file.
 * <p>
 * Place this annotation on a DTO class to enable CSV-to-DTO mapping.
 * The annotation attributes configure how the CSV file is parsed,
 * and can be overridden per-DTO or via {@link dev.gmky.utils.csv.config.CsvReaderConfig}.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @CsvRecord(delimiter = ';', hasHeader = true)
 * @Data
 * public class UserDto {
 *     @CsvColumn("Name")
 *     private String name;
 * }
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CsvRecord {

    /** Column delimiter character. Defaults to comma. */
    char delimiter() default ',';

    /** Whether the first row is a header row. Defaults to true. */
    boolean hasHeader() default true;

    /** File encoding. Defaults to UTF-8. */
    String encoding() default "UTF-8";

    /** Whether to skip empty lines. Defaults to true. */
    boolean skipEmptyLines() default true;

    /** Quote character for quoted fields. Defaults to double-quote. */
    char quoteChar() default '"';

    /** Escape character for special characters. Defaults to backslash. */
    char escapeChar() default '\\';
}
