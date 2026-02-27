package dev.gmky.utils.csv.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExceptionsTest {

    @Test
    void testCsvParsingException() {
        CsvParsingException ex1 = new CsvParsingException(10, "raw10", "message only", null);
        assertThat(ex1.getLineNumber()).isEqualTo(10);
        assertThat(ex1.getRawLine()).isEqualTo("raw10");

        Exception cause = new RuntimeException("cause");
        CsvParsingException ex2 = new CsvParsingException(20, "raw20", "message with cause", cause);
        assertThat(ex2.getCause()).isEqualTo(cause);
    }

    @Test
    void testCsvMappingException() {
        CsvMappingException ex1 = new CsvMappingException(15, "fieldA", "valA", "message only");
        assertThat(ex1.getLineNumber()).isEqualTo(15);
        assertThat(ex1.getFieldName()).isEqualTo("fieldA");
        assertThat(ex1.getRawValue()).isEqualTo("valA");
        
        Exception cause = new RuntimeException("cause");
        CsvMappingException ex2 = new CsvMappingException(25, "fieldB", "valB", "message with cause", cause);
        assertThat(ex2.getCause()).isEqualTo(cause);
    }

    @Test
    void testCsvValidationException() {
        CsvValidationException ex = new CsvValidationException(30, java.util.List.of("val error"));
        assertThat(ex.getLineNumber()).isEqualTo(30);
        assertThat(ex.getViolations()).containsExactly("val error");
    }
}
