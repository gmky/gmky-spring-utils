package dev.gmky.utils.csv.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a DTO field from CSV mapping.
 * <p>
 * Fields annotated with {@code @CsvIgnore} are skipped during both
 * reading and any future write operations. Useful for computed fields,
 * audit timestamps, or sensitivity fields that should not be loaded from CSV.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @CsvIgnore
 * private LocalDateTime createdAt;
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CsvIgnore {
}
