package dev.gmky.utils.logging.http.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLoggingHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(HttpLoggingHelperTest.class);

    // ---- formatHeaders --------------------------------------------------------

    @Test
    void formatHeaders_nullHeaders_shouldReturnNone() {
        String result = HttpLoggingHelper.formatHeaders(null, List.of());
        assertThat(result).isEqualTo("(none)");
    }

    @Test
    void formatHeaders_emptyHeaders_shouldReturnNone() {
        String result = HttpLoggingHelper.formatHeaders(new HttpHeaders(), List.of());
        assertThat(result).isEqualTo("(none)");
    }

    @Test
    void formatHeaders_shouldRedactExcludedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer secret");
        headers.add("Content-Type", "application/json");

        String result = HttpLoggingHelper.formatHeaders(headers, List.of("Authorization"));

        assertThat(result).contains("Content-Type: application/json");
        assertThat(result).contains("Authorization: [REDACTED]");
        assertThat(result).doesNotContain("Bearer secret");
    }

    @Test
    void formatHeaders_redactionIsCaseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("AUTHORIZATION", "Bearer token");

        String result = HttpLoggingHelper.formatHeaders(headers, List.of("authorization"));

        assertThat(result).contains("[REDACTED]");
        assertThat(result).doesNotContain("Bearer token");
    }

    // ---- truncateBody ---------------------------------------------------------

    @Test
    void truncateBody_blank_shouldReturnNull() {
        assertThat(HttpLoggingHelper.truncateBody("", 100)).isNull();
        assertThat(HttpLoggingHelper.truncateBody("   ", 100)).isNull();
        assertThat(HttpLoggingHelper.truncateBody(null, 100)).isNull();
    }

    @Test
    void truncateBody_withinLimit_shouldReturnFull() {
        String body = "hello";
        assertThat(HttpLoggingHelper.truncateBody(body, 100)).isEqualTo("hello");
    }

    @Test
    void truncateBody_exceededLimit_shouldTruncate() {
        String body = "0123456789";
        String result = HttpLoggingHelper.truncateBody(body, 5);
        assertThat(result).startsWith("01234");
        assertThat(result).endsWith("...[truncated]");
    }

    // ---- matchesExcludePath --------------------------------------------------

    @Test
    void matchesExcludePath_exactMatch() {
        assertThat(HttpLoggingHelper.matchesExcludePath("/health", List.of("/health"))).isTrue();
    }

    @Test
    void matchesExcludePath_antWildcard() {
        assertThat(HttpLoggingHelper.matchesExcludePath("/actuator/health",
                List.of("/actuator/**"))).isTrue();
    }

    @Test
    void matchesExcludePath_noMatch() {
        assertThat(HttpLoggingHelper.matchesExcludePath("/api/users",
                List.of("/actuator/**", "/health"))).isFalse();
    }

    @Test
    void matchesExcludePath_nullOrEmptyPatterns_shouldReturnFalse() {
        assertThat(HttpLoggingHelper.matchesExcludePath("/api/users", null)).isFalse();
        assertThat(HttpLoggingHelper.matchesExcludePath("/api/users", List.of())).isFalse();
        assertThat(HttpLoggingHelper.matchesExcludePath(null, List.of("/api/**"))).isFalse();
    }

    // ---- isBinaryContent ----------------------------------------------------

    @Test
    void isBinaryContent_imageJpeg_shouldBeTrue() {
        assertThat(HttpLoggingHelper.isBinaryContent("image/jpeg")).isTrue();
    }

    @Test
    void isBinaryContent_multipartFormData_shouldBeTrue() {
        assertThat(HttpLoggingHelper.isBinaryContent("multipart/form-data; boundary=----")).isTrue();
    }

    @Test
    void isBinaryContent_applicationJson_shouldBeFalse() {
        assertThat(HttpLoggingHelper.isBinaryContent("application/json")).isFalse();
    }

    @Test
    void isBinaryContent_null_shouldBeFalse() {
        assertThat(HttpLoggingHelper.isBinaryContent(null)).isFalse();
    }

    // ---- logAtLevel ----------------------------------------------------------

    @Test
    void logAtLevel_allLevels_shouldNotThrow() {
        for (String level : java.util.Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "UNKNOWN", null)) {
            HttpLoggingHelper.logAtLevel(LOG, level, "Test log at level {}", level);
        }
    }

    // ---- New tests for missing branch coverage ----

    @Test
    void formatHeaders_withNullExcludeList_shouldNotThrow() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        String result = HttpLoggingHelper.formatHeaders(headers, null);

        assertThat(result).contains("Content-Type");
        assertThat(result).doesNotContain("[REDACTED]");
    }

    @Test
    void isBinaryContent_audioType_shouldBeTrue() {
        assertThat(HttpLoggingHelper.isBinaryContent("audio/mpeg")).isTrue();
    }

    @Test
    void isBinaryContent_videoType_shouldBeTrue() {
        assertThat(HttpLoggingHelper.isBinaryContent("video/mp4")).isTrue();
    }

    @Test
    void isBinaryContent_octetStream_shouldBeTrue() {
        assertThat(HttpLoggingHelper.isBinaryContent("application/octet-stream")).isTrue();
    }

    @Test
    void isBinaryContent_textPlain_shouldBeFalse() {
        assertThat(HttpLoggingHelper.isBinaryContent("text/plain")).isFalse();
    }

    @Test
    void binaryContentLabel_shouldReturnExpectedString() {
        assertThat(HttpLoggingHelper.binaryContentLabel()).isEqualTo("[binary content]");
    }

    @Test
    void truncateBody_atExactBoundary_shouldNotTruncate() {
        String body = "12345"; // exactly 5 bytes
        String result = HttpLoggingHelper.truncateBody(body, 5);
        assertThat(result).isEqualTo("12345");
        assertThat(result).doesNotContain("...[truncated]");
    }

    @Test
    void formatHeaders_multipleValuesPerHeader_shouldJoinWithComma() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        headers.add("Accept", "text/plain");

        String result = HttpLoggingHelper.formatHeaders(headers, List.of());

        assertThat(result).contains("Accept");
        assertThat(result).contains("application/json");
        assertThat(result).contains("text/plain");
    }
}
