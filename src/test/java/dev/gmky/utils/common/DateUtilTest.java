package dev.gmky.utils.common;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void formatDate_ShouldReturnFormattedString_WhenDateAndPatternAreValid() {
        // Note: Date.from(Instant) uses system default zone when converted back to string usually, 
        // but DateUtil uses date.toInstant() and DateTimeFormatter.
        // Instant cannot be formatted with pattern "yyyy-MM-dd" without a ZoneId 
        // because it doesn't have human time fields. 
        // We suspect the implementation might fail for pattern-based formatting on Instant 
        // if not handled with a Zone.
        // Let's test with a pattern that relies on Zone to see if it fails (it likely will).
        // Or maybe the pattern "ISO_INSTANT" works.

        Date date = new Date(1706774400000L); // 2024-02-01 08:00:00 UTC (approx)
        String pattern = "yyyy-MM-dd";

        // This execution is expected to fail with UnsupportedTemporalTypeException 
        // if the implementation just does formatter.format(instant).
        // Let's see what happens.

        String result = DateUtil.formatDate(date, pattern);

        // Assert
        // Result depends on local timezone, but we can verify it's not null and matches length/format roughly
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}")); // yyyy-MM-dd
    }

    @Test
    void formatDate_ShouldReturnNull_WhenDateIsNull() {
        String result = DateUtil.formatDate(null, "yyyy-MM-dd");
        assertNull(result);
    }
}
