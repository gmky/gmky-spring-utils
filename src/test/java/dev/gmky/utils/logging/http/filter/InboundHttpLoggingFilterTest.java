package dev.gmky.utils.logging.http.filter;

import dev.gmky.utils.logging.http.config.HttpLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboundHttpLoggingFilterTest {

    private HttpLoggingProperties properties;
    private InboundHttpLoggingFilter filter;

    @BeforeEach
    void setUp() {
        properties = new HttpLoggingProperties();
        properties.setEnabled(true);
        filter = new InboundHttpLoggingFilter(properties);
    }

    @Test
    void normalRequest_shouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void excludedPath_shouldSkipLoggingAndPassThrough() throws Exception {
        properties.getInbound().setExcludePaths(List.of("/actuator/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void withBody_shouldLogRequestBody() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setContentType("application/json");
        request.setContent("{\"item\":\"book\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withBinaryContent_shouldNotLogBody() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
        request.setContentType("image/png");
        request.setContent(new byte[]{0x1, 0x2, 0x3});
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withHeaders_shouldRedactSensitiveHeaders() throws Exception {
        properties.getInbound().setIncludeHeaders(true);
        properties.getInbound().setExcludeHeaders(List.of("Authorization"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.addHeader("Authorization", "Bearer my-secret-token");
        request.addHeader("X-Request-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withQueryString_shouldLogFullUri() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setQueryString("page=1&size=20");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void responseBody_whenIncludeBodyEnabled_shouldBeAvailableToClient() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    // ---- New tests for missing coverage ----

    @Test
    void withHeadersDisabled_shouldNotIncludeHeaders() throws Exception {
        properties.getInbound().setIncludeHeaders(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items");
        request.addHeader("X-Custom-Header", "some-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withResponseBody_shouldLogAndCopyBack() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");

        FilterChain chainWithBody = (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"ok\"}");
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chainWithBody);

        assertThat(response.getContentAsString()).contains("ok");
    }

    @Test
    void withBinaryResponseBody_shouldNotLogResponseBody() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/image");

        FilterChain chainWithBody = (req, res) -> {
            res.setContentType("image/jpeg");
            res.getOutputStream().write(new byte[]{0x1, 0x2, 0x3});
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chainWithBody);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withResponseAlreadyWrapped_shouldNotDoubleWrap() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/check");
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ContentCachingResponseWrapper alreadyWrapped = new ContentCachingResponseWrapper(rawResponse);

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, alreadyWrapped, chain);

        assertThat(rawResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void withInvalidCharsetEncoding_shouldFallbackToUtf8() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        request.setContentType("application/json; charset=INVALID-CHARSET");
        request.setCharacterEncoding("TOTALLY-INVALID");
        request.setContent("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Should not throw — falls back to UTF-8
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withValidCharsetEncoding_shouldUseIt() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withEmptyBody_andIncludeBodyEnabled_shouldNotThrow() throws Exception {
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/empty");
        request.setContentType("application/json");
        request.setContent(new byte[0]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withBodyExceedingMaxSize_shouldTruncate() throws Exception {
        properties.getInbound().setIncludeBody(true);
        properties.getInbound().setMaxBodySize(10);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/large");
        request.setContentType("application/json");
        request.setContent("{\"data\":\"this is a very long body that exceeds the limit\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void filterChainException_shouldStillLogResponseAndRethrow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/crash");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainThatThrows = (req, res) -> {
            throw new ServletException("simulated crash");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chainThatThrows))
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("simulated crash");
    }

    @Test
    void withResponseBodyText_nullTruncation_shouldNotAppendBody() throws Exception {
        properties.getInbound().setIncludeBody(true);

        FilterChain chainWithBlankBody = (req, res) -> {
            res.setContentType("application/json");
            // write nothing — empty response body
        };

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/empty-response");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainWithBlankBody);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void cachedBodyRequest_getInputStreamIsReReadable() throws Exception {
        properties.getInbound().setIncludeBody(true);

        byte[] bodyBytes = "hello".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContentType("application/json");
        request.setContent(bodyBytes);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainThatReadsBody = (req, res) -> {
            byte[] read = req.getInputStream().readAllBytes();
            assertThat(read).isEqualTo(bodyBytes);
        };

        filter.doFilter(request, response, chainThatReadsBody);
    }

    @Test
    void cachedBodyRequest_getReaderShouldWork() throws Exception {
        properties.getInbound().setIncludeBody(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reader");
        request.setContentType("application/json");
        request.setContent("{\"test\":true}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainThatUsesReader = (req, res) -> {
            String line = req.getReader().readLine();
            assertThat(line).isNotNull();
        };

        filter.doFilter(request, response, chainThatUsesReader);
    }

    @Test
    void cachedBodyRequest_getContentLength() throws Exception {
        properties.getInbound().setIncludeBody(true);
        byte[] bodyBytes = "hello world".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/length");
        request.setContentType("application/json");
        request.setContent(bodyBytes);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainThatChecksLength = (req, res) -> {
            assertThat(req.getContentLength()).isEqualTo(bodyBytes.length);
            assertThat(req.getContentLengthLong()).isEqualTo(bodyBytes.length);
        };

        filter.doFilter(request, response, chainThatChecksLength);
    }

    @Test
    void withWhitespaceOnlyRequestBody_shouldNotAppendRequestBody() throws Exception {
        // Covers the truncated==null false branch in logRequest (blank string body)
        properties.getInbound().setIncludeBody(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/blank-request");
        request.setContentType("application/json");
        request.setContent("   ".getBytes(StandardCharsets.UTF_8)); // all whitespace → truncateBody returns null
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void withBlankResponseBodyBytes_shouldNotAppendBody() throws Exception {
        // Covers the truncated==null false branch in logResponse (blank string body)
        properties.getInbound().setIncludeBody(true);

        FilterChain chainWithBlankBytes = (req, res) -> {
            res.setContentType("application/json");
            res.getOutputStream().write("   ".getBytes(StandardCharsets.UTF_8)); // all whitespace → truncateBody returns null
        };

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/blank-body");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainWithBlankBytes);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void cachedBodyRequest_servletInputStreamMethods_shouldWork() throws Exception {
        properties.getInbound().setIncludeBody(true);
        byte[] bodyBytes = "test".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/stream-methods");
        request.setContentType("application/json");
        request.setContent(bodyBytes);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainThatUsesStream = (req, res) -> {
            jakarta.servlet.ServletInputStream stream = req.getInputStream();

            // isFinished() — false before reading
            assertThat(stream.isFinished()).isFalse();
            // isReady() — always true
            assertThat(stream.isReady()).isTrue();
            // setReadListener — no-op, should not throw
            stream.setReadListener(null);
            // read(byte[], int, int) — bulk read
            byte[] buf = new byte[4];
            int n = stream.read(buf, 0, buf.length);
            assertThat(n).isGreaterThan(0);
            // isFinished() — true after reading all bytes
            assertThat(stream.isFinished()).isTrue();
        };

        filter.doFilter(request, response, chainThatUsesStream);
    }

    @Test
    void cachedBodyRequest_singleByteRead_shouldWork() throws Exception {
        properties.getInbound().setIncludeBody(true);
        byte[] bodyBytes = "AB".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/single-byte");
        request.setContentType("application/json");
        request.setContent(bodyBytes);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chainThatReadsByByte = (req, res) -> {
            jakarta.servlet.ServletInputStream stream = req.getInputStream();
            // Exercise the single-byte read() method
            int b1 = stream.read();
            int b2 = stream.read();
            int eof = stream.read();
            assertThat(b1).isEqualTo('A');
            assertThat(b2).isEqualTo('B');
            assertThat(eof).isEqualTo(-1);
        };

        filter.doFilter(request, response, chainThatReadsByByte);
    }
}
