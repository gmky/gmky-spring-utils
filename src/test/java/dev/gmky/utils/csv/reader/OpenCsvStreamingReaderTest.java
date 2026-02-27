package dev.gmky.utils.csv.reader;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvDateFormat;
import dev.gmky.utils.csv.annotation.CsvIgnore;
import dev.gmky.utils.csv.annotation.CsvRecord;
import dev.gmky.utils.csv.callback.CsvReadCallback;
import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.mapper.CsvRowMapper;
import dev.gmky.utils.csv.model.CsvReadResult;
import dev.gmky.utils.csv.validator.CsvRowValidator;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenCsvStreamingReaderTest {

    // ---- test DTOs ----

    @Data
    @CsvRecord
    static class UserDto {
        @CsvColumn("Name")
        private String name;

        @CsvColumn(value = "Email", required = true)
        private String email;

        @CsvColumn("Age")
        private Integer age;

        @CsvColumn("Active")
        private Boolean active;

        @CsvColumn("Salary")
        private BigDecimal salary;

        @CsvColumn("Joined")
        @CsvDateFormat(pattern = "dd/MM/yyyy")
        private LocalDate joined;

        @CsvIgnore
        private String ignored = "should-not-change";
    }

    @Data
    static class NoRecordDto {
        @CsvColumn(index = 0)
        private String col;
    }

    // ---- CSV content ----

    private static final String SIMPLE_CSV =
            "Name,Email,Age,Active,Salary,Joined\n" +
            "Alice,alice@example.com,30,true,5000.00,01/06/2020\n" +
            "Bob,bob@example.com,25,false,3500.00,15/01/2019\n";

    private static final String QUOTED_CSV =
            "Name,Email,Age,Active,Salary,Joined\n" +
            "\"Alice, Jr.\",alice@example.com,30,true,5000.00,01/06/2020\n";

    private static final String MULTILINE_CSV =
            "Name,Email,Age,Active,Salary,Joined\n" +
            "\"Alice\nMultiline\",alice@example.com,30,true,5000.00,01/06/2020\n";

    private static final String MISSING_REQUIRED_CSV =
            "Name,Email,Age,Active,Salary,Joined\n" +
            "Bob,,25,true,1000.00,01/01/2020\n";

    private InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // ---- tests ----

    @Test
    void readAll_shouldMapAllFieldsCorrectly() {
        // Use FAIL_FAST to surface any hidden mapping errors during diagnosis
        var config = CsvReaderConfig.builder()
                .errorStrategy(CsvReaderConfig.ErrorStrategy.FAIL_FAST).build();
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);
        List<UserDto> users = reader.readAll(csv(SIMPLE_CSV));

        assertThat(users).hasSize(2);

        UserDto alice = users.get(0);
        assertThat(alice.getName()).isEqualTo("Alice");
        assertThat(alice.getEmail()).isEqualTo("alice@example.com");
        assertThat(alice.getAge()).isEqualTo(30);
        assertThat(alice.getActive()).isTrue();
        assertThat(alice.getJoined()).isEqualTo(LocalDate.of(2020, 6, 1));
        assertThat(alice.getIgnored()).isEqualTo("should-not-change");
    }

    @Test
    void readAll_shouldHandleQuotedFields() {
        var reader = OpenCsvStreamingReader.forType(UserDto.class);
        List<UserDto> users = reader.readAll(csv(QUOTED_CSV));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("Alice, Jr.");
    }

    @Test
    void readAll_shouldHandleMultilineQuotedFields() {
        var reader = OpenCsvStreamingReader.forType(UserDto.class);
        List<UserDto> users = reader.readAll(csv(MULTILINE_CSV));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("Alice\nMultiline");
    }

    @Test
    void stream_shouldReturnLazyStream() {
        var reader = OpenCsvStreamingReader.forType(UserDto.class);
        try (Stream<UserDto> stream = reader.stream(csv(SIMPLE_CSV))) {
            long count = stream.filter(u -> u.getAge() > 26).count();
            assertThat(count).isEqualTo(1);
        }
    }

    @Test
    void stream_shouldCatchMappingExceptions() {
        var config = CsvReaderConfig.builder().hasHeader(false).build();
        CsvRowMapper<UserDto> failingMapper = (c, h, l) -> {
            if (c[0].equals("FAIL")) throw new RuntimeException("Test err");
            return new UserDto();
        };
        var reader = OpenCsvStreamingReader.withMapper(UserDto.class, config, failingMapper);
        
        List<UserDto> results = reader.stream(csv("OK\nFAIL\nOK")).toList();
        
        assertThat(results).hasSize(2);
    }

    @Test
    void read_shouldInvokeConsumerForEachRow() {
        var reader = OpenCsvStreamingReader.forType(UserDto.class);
        List<String> names = new java.util.ArrayList<>();
        reader.read(csv(SIMPLE_CSV), u -> names.add(u.getName()));

        assertThat(names).containsExactly("Alice", "Bob");
    }

    @Test
    void readWithResult_shouldTrackErrorsOnSkipAndLog() {
        var config = CsvReaderConfig.builder()
                .errorStrategy(CsvReaderConfig.ErrorStrategy.SKIP_AND_LOG).build();
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);

        CsvReadResult<UserDto> result = reader.readWithResult(csv(MISSING_REQUIRED_CSV));

        assertThat(result.successCount()).isZero();
        assertThat(result.errorCount()).isGreaterThan(0);
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.elapsed()).isNotNull();
    }

    @Test
    void readWithResult_shouldThrowOnFailFast() {
        var config = CsvReaderConfig.builder()
                .errorStrategy(CsvReaderConfig.ErrorStrategy.FAIL_FAST).build();
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);

        assertThatThrownBy(() -> reader.readWithResult(csv(MISSING_REQUIRED_CSV)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void readAll_withEmptyFile_shouldReturnEmptyList() {
        var reader = OpenCsvStreamingReader.forType(UserDto.class);
        List<UserDto> users = reader.readAll(csv("Name,Email,Age,Active,Salary,Joined\n"));
        assertThat(users).isEmpty();
    }

    @Test
    void resolveConfig_shouldWorkWithoutAnnotation() {
        var reader = OpenCsvStreamingReader.forType(NoRecordDto.class);
        List<NoRecordDto> results = reader.readAll(csv("val1\nval2\n"));
        // hasHeader defaults to true, so row 1 is skipped
        assertThat(results).hasSize(1);
    }

    @Test
    void readAll_withCustomDelimiter_shouldParseSemicolonSeparated() {
        String semiColonCsv =
                "Name;Email;Age;Active;Salary;Joined\n" +
                "Charlie;charlie@example.com;40;true;9000.00;01/01/2018\n";
        var config = CsvReaderConfig.builder().delimiter(';').build();
        // Override @CsvRecord which defaults to comma
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);
        List<UserDto> users = reader.readAll(csv(semiColonCsv));

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("Charlie");
    }

    @Test
    void readAll_withMaxErrors_shouldStopEarlyWhenLimitReached() {
        String csvContent =
                "Name,Email,Age,Active,Salary,Joined\n" +
                "A,,1,true,100.00,01/01/2020\n" +
                "B,,2,true,200.00,01/01/2020\n" +
                "C,,3,true,300.00,01/01/2020\n";
        var config = CsvReaderConfig.builder()
                .errorStrategy(CsvReaderConfig.ErrorStrategy.SKIP_AND_LOG)
                .maxErrors(1).build();
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);
        CsvReadResult<UserDto> result = reader.readWithResult(csv(csvContent));

        assertThat(result.errorCount()).isLessThanOrEqualTo(1);
    }

    @Test
    void shouldThrowExceptionOnStreamIOError() {
        var reader = OpenCsvStreamingReader.forType(UserDto.class);
        
        assertThatThrownBy(() -> reader.readAll(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read CSV");
                
        assertThatThrownBy(() -> reader.stream(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to open/read");
    }

    @Test
    void shouldHitUntouchedBranches() {
        String csvContent = " \n A, ,1,true,100, \n \n";
        
        var config = CsvReaderConfig.builder()
                .hasHeader(false)
                .trimValues(false)
                .skipEmptyLines(false)
                .parallel(true)
                .errorStrategy(CsvReaderConfig.ErrorStrategy.SKIP_SILENT)
                .build();
                
        CsvRowMapper<UserDto> mapper = (cols, hdrs, line) -> new UserDto();
                
        var reader = OpenCsvStreamingReader.withMapper(UserDto.class, config, mapper);
        
        // This will test: no headers, no trim, don't skip empty, parallel stream, silent errors.
        List<UserDto> users = reader.readAll(csv(csvContent));
        
        // Due to skipEmptyLines=false and the content above:
        // row 1: [" "]
        // row 2: [" A ", " ", "1", "true", "100", " "]
        // row 3: [" "]
        assertThat(users).hasSize(3); // assuming mapper never throws
        
        // also test stream parallel explicitly
        long count = reader.stream(csv(csvContent)).count();
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldExecuteWithCustomMapper() {
        CsvRowMapper<UserDto> mapper = (columns, headers, line) -> {
            UserDto dto = new UserDto();
            dto.setName("Custom_" + columns[0]);
            return dto;
        };
        var reader = OpenCsvStreamingReader.withMapper(UserDto.class, CsvReaderConfig.defaultConfig(), mapper);
        List<UserDto> results = reader.readAll(csv(SIMPLE_CSV));
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getName()).startsWith("Custom_");
    }

    @Test
    void shouldFailOnValidationViolation() {
        CsvRowValidator<UserDto> validator = record -> 
            record.getName().equals("Alice") ? Collections.emptyList() : List.of("Invalid name");

        var config = CsvReaderConfig.builder()
                .validator(validator)
                .errorStrategy(CsvReaderConfig.ErrorStrategy.SKIP_AND_LOG).build();
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);
        
        CsvReadResult<UserDto> result = reader.readWithResult(csv(SIMPLE_CSV));
        assertThat(result.successCount()).isEqualTo(1); // Alice passes
        assertThat(result.errorCount()).isEqualTo(1); // Bob fails
        
        // Test Fail Fast
        var configFailFast = CsvReaderConfig.builder()
                .validator(validator)
                .errorStrategy(CsvReaderConfig.ErrorStrategy.FAIL_FAST).build();
        var failFastReader = OpenCsvStreamingReader.forType(UserDto.class, configFailFast);
        
        assertThatThrownBy(() -> failFastReader.readWithResult(csv(SIMPLE_CSV)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid name");
    }

    @Test
    void shouldExecuteCallbacks() {
        List<String> events = new ArrayList<>();
        CsvReadCallback<UserDto> callback = new CsvReadCallback<>() {
            @Override public void onStart(CsvReaderConfig config) { events.add("start"); }
            @Override public void onHeader(String[] headers) { events.add("header"); }
            @Override public void onRow(long lineNumber, UserDto record) { events.add("row"); }
            @Override public void onComplete(CsvReadResult<UserDto> result) { events.add("complete"); }
        };
        var config = CsvReaderConfig.builder().callback(callback).build();
        var reader = OpenCsvStreamingReader.forType(UserDto.class, config);
        
        reader.readAll(csv(SIMPLE_CSV));
        
        assertThat(events).containsExactly("start", "header", "row", "row", "complete");
    }

    @Test
    void shouldReadWithoutHeader() {
        String noHeaderCsv = "Alice,alice@example.com,30,true,5000.00,01/06/2020";
        var config = CsvReaderConfig.builder().hasHeader(false).build();
        CsvRowMapper<UserDto> mapper = (columns, headers, line) -> {
            UserDto dto = new UserDto();
            dto.setName(columns[0]);
            return dto;
        };
        var reader = OpenCsvStreamingReader.withMapper(UserDto.class, config, mapper);
        List<UserDto> users = reader.readAll(csv(noHeaderCsv));
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("Alice");
    }
}
