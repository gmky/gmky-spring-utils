package dev.gmky.utils.csv.converter.impl;

import dev.gmky.utils.csv.annotation.CsvDateFormat;
import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Built-in converter for Java temporal types:
 * {@link LocalDate}, {@link LocalDateTime}, {@link LocalTime}, {@link Instant}, {@link ZonedDateTime}.
 * <p>
 * Uses the pattern from {@link CsvDateFormat} on the field, or ISO defaults.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class TemporalConverter implements TypeConverter<Object> {

    @Override
    public Object convert(String value, CsvFieldMeta meta) {
        if (StringUtils.isBlank(value)) return null;
        String trimmed = value.trim();
        Class<?> fieldType = meta.getFieldType();
        DateTimeFormatter formatter = meta.getDateTimeFormatter();

        if (fieldType == LocalDate.class) return LocalDate.parse(trimmed, formatter);
        if (fieldType == LocalDateTime.class) return LocalDateTime.parse(trimmed, formatter);
        if (fieldType == LocalTime.class) return LocalTime.parse(trimmed, formatter);
        if (fieldType == ZonedDateTime.class) return ZonedDateTime.parse(trimmed, formatter);
        if (fieldType == Instant.class) {
            // For Instant, parse as ZonedDateTime then convert
            String tz = meta.getDateFormatTimezone();
            ZoneId zone = StringUtils.isBlank(tz) ? ZoneId.systemDefault() : ZoneId.of(tz);
            try {
                return LocalDateTime.parse(trimmed, formatter).atZone(zone).toInstant();
            } catch (java.time.DateTimeException e) {
                // Fallback if formatter only provides date
                return LocalDate.parse(trimmed, formatter).atStartOfDay(zone).toInstant();
            }
        }

        throw new IllegalArgumentException("Unsupported temporal type: " + fieldType.getName());
    }

    @Override
    public Class<Object> getTargetType() {
        // Handles multiple types; type routing is done in convert()
        return Object.class;
    }

    /**
     * Returns true if this converter can handle the given type.
     */
    public static boolean supports(Class<?> type) {
        return type == LocalDate.class || type == LocalDateTime.class
                || type == LocalTime.class || type == ZonedDateTime.class
                || type == Instant.class;
    }
}
