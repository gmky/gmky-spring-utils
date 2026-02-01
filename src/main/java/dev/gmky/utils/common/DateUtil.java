package dev.gmky.utils.common;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Utility class for date and time operations.
 * <p>
 * Provides helper methods for formatting dates using {@link java.time} API.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
@Slf4j
@UtilityClass
public class DateUtil {
    /**
     * Formats a {@link Date} object into a string using the specified pattern.
     * <p>
     * Uses the system default time zone for conversion.
     * </p>
     *
     * @param date    the date to format
     * @param pattern the pattern to use (e.g., "yyyy-MM-dd")
     * @return the formatted date string, or null if the input date is null
     */
    public String formatDate(Date date, String pattern) {
        if (date == null) {
            log.warn("Date is null");
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(java.time.ZoneId.systemDefault());
        return formatter.format(date.toInstant());
    }
}
