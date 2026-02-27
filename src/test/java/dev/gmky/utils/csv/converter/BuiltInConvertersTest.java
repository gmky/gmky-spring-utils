package dev.gmky.utils.csv.converter;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvDateFormat;
import dev.gmky.utils.csv.converter.impl.*;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for all built-in {@link TypeConverter} implementations.
 */
class BuiltInConvertersTest {

    // ---- helper DTO for meta building ----
    @SuppressWarnings("unused")
    static class SampleDto {
        @CsvColumn("str") String strField;
        @CsvColumn("num") Integer intField;
        @CsvColumn("lng") Long longField;
        @CsvColumn("dbl") Double dblField;
        @CsvColumn("bool") Boolean boolField;
        @CsvColumn("bd") BigDecimal bdField;
        @CsvColumn("date") @CsvDateFormat(pattern = "dd/MM/yyyy") LocalDate dateField;
        @CsvColumn("dt") @CsvDateFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime dtField;
        @CsvColumn("time") @CsvDateFormat(pattern = "HH:mm") LocalTime timeField;
        @CsvColumn("zt") @CsvDateFormat(pattern = "dd/MM/yyyy HH:mm XXX") ZonedDateTime ztField;
        @CsvColumn("inst") @CsvDateFormat(pattern = "dd/MM/yyyy HH:mm", timezone = "UTC") Instant instField;
        @CsvColumn("defInst") Instant defInstField;
        @CsvColumn("flt") Float floatField;
        @CsvColumn("sht") Short shortField;
        @CsvColumn("byt") Byte byteField;
        @CsvColumn("p_int") int pIntField;
        @CsvColumn("obj") Object objField;
        @CsvColumn("status") Status statusField;
    }

    enum Status { ACTIVE, INACTIVE }

    private CsvFieldMeta meta(String fieldName) throws Exception {
        Field f = SampleDto.class.getDeclaredField(fieldName);
        CsvColumn col = f.getAnnotation(CsvColumn.class);
        CsvDateFormat df = f.getAnnotation(CsvDateFormat.class);
        return new CsvFieldMeta(f, col, df);
    }

    // ---- StringConverter ----

    @Test
    void stringConverter_shouldReturnValueAsIs() throws Exception {
        var conv = new StringConverter();
        assertThat(conv.convert("hello", meta("strField"))).isEqualTo("hello");
        assertThat(conv.getTargetType()).isEqualTo(String.class);
    }

    // ---- NumberConverter ----

    @Test
    void numberConverter_shouldParseInteger() throws Exception {
        var conv = new NumberConverter();
        assertThat(conv.convert("42", meta("intField"))).isEqualTo(42);
    }

    @Test
    void numberConverter_shouldParseLong() throws Exception {
        var conv = new NumberConverter();
        assertThat(conv.convert("9999999999", meta("longField"))).isEqualTo(9999999999L);
    }

    @Test
    void numberConverter_shouldParseDouble() throws Exception {
        var conv = new NumberConverter();
        assertThat(conv.convert("3.14", meta("dblField"))).isEqualTo(3.14);
    }

    @Test
    void numberConverter_shouldParseOtherTypes() throws Exception {
        var conv = new NumberConverter();
        assertThat(conv.convert("3.14", meta("floatField"))).isEqualTo(3.14f);
        assertThat(conv.convert("42", meta("shortField"))).isEqualTo((short) 42);
        assertThat(conv.convert("42", meta("byteField"))).isEqualTo((byte) 42);
        assertThat(conv.convert("42", meta("pIntField"))).isEqualTo(42);
    }

    @Test
    void numberConverter_shouldRejectUnsupported() throws Exception {
        var conv = new NumberConverter();
        assertThatThrownBy(() -> conv.convert("42", meta("objField")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numberConverter_shouldReturnNullForBlank() throws Exception {
        var conv = new NumberConverter();
        assertThat(conv.convert("  ", meta("intField"))).isNull();
    }

    // ---- BooleanConverter ----

    @Test
    void booleanConverter_shouldMapTruthyValues() throws Exception {
        var conv = new BooleanConverter();
        var m = meta("boolField");
        assertThat(conv.convert("true", m)).isTrue();
        assertThat(conv.convert("yes", m)).isTrue();
        assertThat(conv.convert("1", m)).isTrue();
        assertThat(conv.convert("on", m)).isTrue();
        assertThat(conv.convert("TRUE", m)).isTrue();
    }

    @Test
    void booleanConverter_shouldMapFalsyValues() throws Exception {
        var conv = new BooleanConverter();
        assertThat(conv.convert("false", meta("boolField"))).isFalse();
        assertThat(conv.convert("no", meta("boolField"))).isFalse();
        assertThat(conv.convert("0", meta("boolField"))).isFalse();
    }

    @Test
    void booleanConverter_shouldReturnNullForBlank() throws Exception {
        var conv = new BooleanConverter();
        assertThat(conv.convert("", meta("boolField"))).isNull();
    }

    // ---- BigDecimalConverter ----

    @Test
    void bigDecimalConverter_shouldStripCurrencySymbols() throws Exception {
        var conv = new BigDecimalConverter();
        assertThat(conv.convert("$1,500.00", meta("bdField"))).isEqualTo(new BigDecimal("1500.00"));
        assertThat(conv.convert("€2500.50", meta("bdField"))).isEqualTo(new BigDecimal("2500.50"));
    }

    @Test
    void bigDecimalConverter_shouldParseSimpleNumber() throws Exception {
        var conv = new BigDecimalConverter();
        assertThat(conv.convert("999.99", meta("bdField"))).isEqualByComparingTo("999.99");
    }

    @Test
    void bigDecimalConverter_shouldReturnNullForBlank() throws Exception {
        var conv = new BigDecimalConverter();
        assertThat(conv.convert("  ", meta("bdField"))).isNull();
    }

    // ---- TemporalConverter ----

    @Test
    void temporalConverter_shouldParseLocalDate() throws Exception {
        var conv = new TemporalConverter();
        assertThat(conv.convert("15/06/2023", meta("dateField")))
                .isEqualTo(LocalDate.of(2023, 6, 15));
    }

    @Test
    void temporalConverter_shouldParseLocalDateTime() throws Exception {
        var conv = new TemporalConverter();
        assertThat(conv.convert("15/06/2023 10:30", meta("dtField")))
                .isEqualTo(LocalDateTime.of(2023, 6, 15, 10, 30));
    }

    @Test
    void temporalConverter_shouldParseOtherTypes() throws Exception {
        var conv = new TemporalConverter();
        assertThat(conv.convert("10:30", meta("timeField")))
                .isEqualTo(LocalTime.of(10, 30));
        
        ZonedDateTime expectedZt = java.time.ZonedDateTime.of(2023, 6, 15, 10, 30, 0, 0, java.time.ZoneId.of("Z"));
        assertThat(conv.convert("15/06/2023 10:30 Z", meta("ztField")))
                .isEqualTo(expectedZt);
        
        assertThat(conv.convert("15/06/2023 10:30", meta("instField")))
                .isEqualTo(expectedZt.toInstant());
                
        assertThat(conv.convert("2023-06-15", meta("defInstField")))
                .isInstanceOf(Instant.class);
    }

    @Test
    void temporalConverter_shouldRejectUnsupported() throws Exception {
        var conv = new TemporalConverter();
        assertThatThrownBy(() -> conv.convert("15/06/2023", meta("objField")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void temporalConverter_shouldReturnNullForBlank() throws Exception {
        var conv = new TemporalConverter();
        assertThat(conv.convert("", meta("dateField"))).isNull();
    }

    // ---- EnumConverter ----

    @Test
    void enumConverter_shouldMatchCaseInsensitive() throws Exception {
        var conv = new EnumConverter();
        assertThat(conv.convert("active", meta("statusField"))).isEqualTo(Status.ACTIVE);
        assertThat(conv.convert("INACTIVE", meta("statusField"))).isEqualTo(Status.INACTIVE);
    }

    @Test
    void enumConverter_shouldThrowForUnknownValue() throws Exception {
        var conv = new EnumConverter();
        assertThatThrownBy(() -> conv.convert("UNKNOWN", meta("statusField")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown enum value");
    }

    // ---- TypeConverterRegistry ----

    @Test
    void registry_shouldReturnRegisteredConverter() {
        var registry = new TypeConverterRegistry();
        var conv = new StringConverter();
        registry.register(conv);
        assertThat(registry.get(String.class)).isSameAs(conv);
    }

    @Test
    void registry_shouldReturnNullForUnregisteredType() {
        var registry = new TypeConverterRegistry();
        assertThat(registry.get(BigDecimal.class)).isNull();
    }

    @Test
    void registry_hasConverter_shouldReturnTrueForRegistered() {
        var registry = new TypeConverterRegistry();
        registry.register(new StringConverter());
        assertThat(registry.hasConverter(String.class)).isTrue();
        assertThat(registry.hasConverter(BigDecimal.class)).isFalse();
    }

    @Test
    void registry_convert_shouldDelegateToConverter() throws Exception {
        var registry = new TypeConverterRegistry();
        registry.register(new StringConverter());
        
        String result = registry.convert("test", String.class, null);
        assertThat(result).isEqualTo("test");
        
        assertThatThrownBy(() -> registry.convert("test", BigDecimal.class, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No TypeConverter registered");
    }
}
