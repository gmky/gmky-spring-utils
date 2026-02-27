package dev.gmky.utils.csv.annotation;

import dev.gmky.utils.csv.converter.TypeConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a DTO field to a CSV column.
 * <p>
 * Supports binding by column header name or by zero-based column index.
 * When {@code hasHeader} is true on the enclosing {@link CsvRecord}, the {@code value}
 * (header name) is preferred. Otherwise, {@code index} is required.
 * </p>
 *
 * <h3>Example (header-based):</h3>
 * <pre>{@code
 * @CsvColumn(value = "Email", required = true)
 * private String email;
 * }</pre>
 *
 * <h3>Example (index-based):</h3>
 * <pre>{@code
 * @CsvColumn(index = 2)
 * private String phone;
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CsvColumn {

    /** Column header name for header-based binding. Defaults to empty (use field name). */
    String value() default "";

    /** Zero-based column index for index-based binding. Defaults to -1 (use name). */
    int index() default -1;

    /** Whether this column is mandatory. A missing or blank value will trigger an error. */
    boolean required() default false;

    /** Default value to use when the column is empty or missing. */
    String defaultValue() default "";

    /**
     * Custom converter to use for this specific field.
     * Defaults to {@link TypeConverter} (marker for "use registry default").
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TypeConverter> converter() default TypeConverter.class;
}
