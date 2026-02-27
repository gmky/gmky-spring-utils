package dev.gmky.utils.csv.reader;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvRecord;
import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.exception.CsvParsingException;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvBatchReaderTest {

    @Data
    @CsvRecord
    static class TestDto {
        @CsvColumn("Name")
        private String name;

        @CsvColumn(value = "Age", required = true)
        private Integer age;
    }

    private ByteArrayResource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8), "test.csv");
    }

    @Test
    void shouldReadAndMapCorrectly() throws Exception {
        String csv = "Name,Age\nJohn,30\nJane,25\n\n";
        CsvBatchReader<TestDto> reader = new CsvBatchReader<>(TestDto.class, resource(csv));
        
        reader.doOpen();

        TestDto first = reader.doRead();
        assertThat(first).isNotNull();
        assertThat(first.getName()).isEqualTo("John");
        assertThat(first.getAge()).isEqualTo(30);

        TestDto second = reader.doRead();
        assertThat(second).isNotNull();
        assertThat(second.getName()).isEqualTo("Jane");
        assertThat(second.getAge()).isEqualTo(25);

        TestDto third = reader.doRead();
        assertThat(third).isNull();

        reader.doClose();
    }

    @Test
    void shouldSkipEmptyRows() throws Exception {
        String csv = "Name,Age\n\n  \nJohn,30\n\n";
        CsvBatchReader<TestDto> reader = new CsvBatchReader<>(TestDto.class, resource(csv));
        reader.doOpen();

        TestDto first = reader.doRead();
        assertThat(first.getName()).isEqualTo("John");

        TestDto second = reader.doRead();
        assertThat(second).isNull();

        reader.doClose();
    }

    @Test
    void shouldThrowExceptionOnMappingFailure() throws Exception {
        String csv = "Name,Age\nJohn,invalid-age\n";
        CsvBatchReader<TestDto> reader = new CsvBatchReader<>(TestDto.class, resource(csv));
        
        reader.doOpen();

        assertThatThrownBy(reader::doRead)
                .isInstanceOf(CsvParsingException.class)
                .hasMessageContaining("John,invalid-age");
                
        reader.doClose();
    }

    @Test
    void shouldAcceptCustomConfig() throws Exception {
        String csv = "Name;Age\nJohn;30\n";
        CsvReaderConfig config = CsvReaderConfig.builder().delimiter(';').build();
        CsvBatchReader<TestDto> reader = new CsvBatchReader<>(TestDto.class, resource(csv), config);
        
        reader.doOpen();

        TestDto first = reader.doRead();
        assertThat(first).isNotNull();
        assertThat(first.getName()).isEqualTo("John");
        
        reader.doClose();
    }
}
