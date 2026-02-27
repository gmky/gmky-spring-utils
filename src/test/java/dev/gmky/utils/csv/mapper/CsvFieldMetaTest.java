package dev.gmky.utils.csv.mapper;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvDateFormat;
import dev.gmky.utils.csv.converter.TypeConverter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFieldMetaTest {

    static class GoodDto {
        @CsvColumn(value = "col", required = true, defaultValue = "def", index = 1)
        @CsvDateFormat(pattern = "yyyy-MM", timezone = "UTC")
        private String goodField;
    }

    public static class BadConverter implements TypeConverter<String> {
        public BadConverter(String noDefaultConstructor) {}
        @Override
        public String convert(String value, CsvFieldMeta meta) { return value; }
        @Override
        public Class<String> getTargetType() { return String.class; }
    }

    static class BadDto {
        @CsvColumn(converter = BadConverter.class)
        private String badConverterField;
    }

    @Test
    void testValidInitializationAndSetValue() throws Exception {
        Field field = GoodDto.class.getDeclaredField("goodField");
        CsvColumn col = field.getAnnotation(CsvColumn.class);
        CsvDateFormat df = field.getAnnotation(CsvDateFormat.class);

        CsvFieldMeta meta = new CsvFieldMeta(field, col, df);

        assertThat(meta.getFieldName()).isEqualTo("goodField");
        assertThat(meta.getColumnName()).isEqualTo("col");
        assertThat(meta.getColumnIndex()).isEqualTo(1);
        assertThat(meta.isRequired()).isTrue();
        assertThat(meta.getDefaultValue()).isEqualTo("def");
        assertThat(meta.getDateFormatPattern()).isEqualTo("yyyy-MM");
        assertThat(meta.getDateFormatTimezone()).isEqualTo("UTC");
        assertThat(meta.getCustomConverter()).isNull();
        assertThat(meta.isIndexBased()).isTrue();

        GoodDto dto = new GoodDto();
        meta.setValue(dto, "test_val");
        assertThat(dto.goodField).isEqualTo("test_val");
        
        // Test setValue error
        assertThatThrownBy(() -> meta.setValue(new Object(), "test_val"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to set field");
    }

    @Test
    void testBadConverterInitialization() throws Exception {
        Field field = BadDto.class.getDeclaredField("badConverterField");
        CsvColumn col = field.getAnnotation(CsvColumn.class);

        assertThatThrownBy(() -> new CsvFieldMeta(field, col, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to instantiate converter");
    }
}
