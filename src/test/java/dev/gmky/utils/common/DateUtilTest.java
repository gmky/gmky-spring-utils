package dev.gmky.utils.common;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void formatDate_ShouldReturnFormattedString_WhenDateAndPatternAreValid() {
        Date date = new Date(1706774400000L); 
        String pattern = "yyyy-MM-dd";
        String result = DateUtil.formatDate(date, pattern);

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}")); // yyyy-MM-dd
    }

    @Test
    void formatDate_ShouldReturnNull_WhenDateIsNull() {
        String result = DateUtil.formatDate(null, "yyyy-MM-dd");
        assertNull(result);
    }

    @Test
    void testFormatDateWithTimePattern() {
        Date date = new Date(1706774400000L); // timestamp
        String result = DateUtil.formatDate(date, "yyyy-MM-dd HH:mm:ss");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testFormatDateWithDifferentPatterns() {
        Date date = new Date(1706774400000L); 
        String result1 = DateUtil.formatDate(date, "dd/MM/yyyy");
        String result2 = DateUtil.formatDate(date, "MM-dd-yyyy");
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.matches("\\d{2}/\\d{2}/\\d{4}"));
        assertTrue(result2.matches("\\d{2}-\\d{2}-\\d{4}"));
    }

    @Test
    void testFormatDateWithSpecificKnownDate() {
        Date date = new Date(1706774400000L); 
        String result = DateUtil.formatDate(date, "yyyy");
        assertEquals("2024", result); // For most timezones it's 2024
    }
}
