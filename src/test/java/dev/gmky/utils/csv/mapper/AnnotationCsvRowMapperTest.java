package dev.gmky.utils.csv.mapper;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvRecord;
import dev.gmky.utils.csv.converter.impl.StringConverter;
import dev.gmky.utils.csv.exception.CsvMappingException;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnotationCsvRowMapperTest {

    @Data
    @CsvRecord
    static class TestDto {
        @CsvColumn(value = "colA", required = true)
        private String colA;

        @CsvColumn(value = "colB", defaultValue = "test")
        private String colB;

        @CsvColumn(index = 2, converter = CustomStrConverter.class)
        private String colC;
        
        @CsvColumn(index = 3)
        private int missingCol;
        
        @CsvColumn(value = "colReq", required = true)
        private String req;

        @CsvColumn(index = 5) private long pl;
        @CsvColumn(index = 6) private short ps;
        @CsvColumn(index = 7) private byte pb;
        @CsvColumn(index = 8) private float pf;
        @CsvColumn(index = 9) private double pd;
        @CsvColumn(index = 10) private java.time.DayOfWeek dayOfWeek;
        @CsvColumn(index = 11) private Long boxedL;
        @CsvColumn(index = 12) private Short boxedS;
        @CsvColumn(index = 13) private Byte boxedB;
        @CsvColumn(index = 14) private Float boxedF;
        @CsvColumn(index = 15) private Double boxedD;
        @CsvColumn(index = 16) private Boolean boxedBool;
        @CsvColumn(index = 17) private boolean primBool;
    }

    static class UnsupportedDto {
        @CsvColumn(index = 0) private java.util.UUID unsupportedType;
    }

    public static class CustomStrConverter extends StringConverter {
        @Override
        public String convert(String value, CsvFieldMeta meta) {
            return "Custom_" + value;
        }
    }

    @Test
    void testMap() throws Exception {
        String[] headers = {"colA", "colB", "idx2", "idx3", "colReq"};
        AnnotationCsvRowMapper<TestDto> mapper = new AnnotationCsvRowMapper<>(TestDto.class, headers);

        String[] validRow = {"A_val", "B_val", "C_val", "123", "reqVal"};
        TestDto dto = mapper.map(validRow, headers, 1);

        assertThat(dto.getColA()).isEqualTo("A_val");
        assertThat(dto.getColB()).isEqualTo("B_val");
        assertThat(dto.getColC()).isEqualTo("Custom_C_val");
        assertThat(dto.getMissingCol()).isEqualTo(123);
        assertThat(dto.getReq()).isEqualTo("reqVal");

        String[] broadRow = {"A", "B", "C", "1", "req", "10", "20", "30", "40.5", "50.5", "MONDAY", "10", "20", "30", "40.5", "50.5", "true", "true"};
        TestDto broadDto = mapper.map(broadRow, headers, 1);
        assertThat(broadDto.getPl()).isEqualTo(10L);
        assertThat(broadDto.getPs()).isEqualTo((short) 20);
        assertThat(broadDto.getPb()).isEqualTo((byte) 30);
        assertThat(broadDto.getPf()).isEqualTo(40.5f);
        assertThat(broadDto.getPd()).isEqualTo(50.5);
        assertThat(broadDto.getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(broadDto.getBoxedL()).isEqualTo(10L);
        assertThat(broadDto.getBoxedS()).isEqualTo((short) 20);
        assertThat(broadDto.getBoxedB()).isEqualTo((byte) 30);
        assertThat(broadDto.getBoxedF()).isEqualTo(40.5f);
        assertThat(broadDto.getBoxedD()).isEqualTo(50.5);
        assertThat(broadDto.getBoxedBool()).isTrue();
        assertThat(broadDto.isPrimBool()).isTrue();

        // Test unsupported type mapping
        AnnotationCsvRowMapper<UnsupportedDto> noConvMapper = new AnnotationCsvRowMapper<>(UnsupportedDto.class, new String[0]);
        assertThatThrownBy(() -> noConvMapper.map(new String[]{"uuid-here"}, new String[0], 1))
                .isInstanceOf(CsvMappingException.class)
                .hasMessageContaining("No converter found");

        // Test missing required header-driven column mapping exception
        String[] shortRow = {"A_val", "B_val", "C_val", "123"}; // misses index 4, colReq
        // In the headerIndexMap, colReq is at index 4. This validRow has length 4, so colIdx=4 is >= length.
        assertThatThrownBy(() -> mapper.map(shortRow, headers, 2))
                .isInstanceOf(CsvMappingException.class)
                .hasMessageContaining("Required column");

        // Test default value
        String[] emptyBRow = {"A_val", "", "C", "5", "reqVal"};
        TestDto dtoWithDefault = mapper.map(emptyBRow, headers, 3);
        assertThat(dtoWithDefault.getColB()).isEqualTo("test");

        // Test required column is blank
        String[] emptyARow = {"  ", "B", "C", "5", "reqVal"};
        assertThatThrownBy(() -> mapper.map(emptyARow, headers, 4))
                .isInstanceOf(CsvMappingException.class)
                .hasMessageContaining("Required field is blank");
    }

    @Data
    static class NoNoArgDto {
        private String a;
        public NoNoArgDto(String a) { this.a = a; }
    }

    @Test
    void testMissingNoArgConstructorThrowsException() {
        String[] headers = {"a"};
        AnnotationCsvRowMapper<NoNoArgDto> mapper = new AnnotationCsvRowMapper<>(NoNoArgDto.class, headers);
        assertThatThrownBy(() -> mapper.map(new String[]{"val"}, headers, 1))
                .isInstanceOf(CsvMappingException.class)
                .hasMessageContaining("Cannot instantiate DTO");
    }
}
