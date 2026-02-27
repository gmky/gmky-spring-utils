package dev.gmky.utils.csv.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the date/time format for a temporal DTO field when mapping from CSV.
 * <p>
 * Place this annotation alongside {@link CsvColumn} on fields of types:
 * {@code LocalDate}, {@code LocalDateTime}, {@code LocalTime}, {@code Instant}, {@code ZonedDateTime}.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @CsvColumn("Joined Date")
 * @CsvDateFormat(pattern = "dd/MM/yyyy")
 * private LocalDate joinedDate;
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CsvDateFormat {

    /** Date/time pattern compatible with {@link java.time.format.DateTimeFormatter}. */
    String pattern() default "yyyy-MM-dd";

    /** Timezone ID (e.g. "UTC", "Asia/Ho_Chi_Minh"). Empty means system default. */
    String timezone() default "";
}
