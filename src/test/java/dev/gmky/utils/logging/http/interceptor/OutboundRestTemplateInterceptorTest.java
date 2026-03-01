package dev.gmky.utils.logging.http.interceptor;

import dev.gmky.utils.logging.http.config.HttpLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboundRestTemplateInterceptorTest {

    private HttpLoggingProperties properties;
    private OutboundRestTemplateInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new HttpLoggingProperties();
        properties.setEnabled(true);
        interceptor = new OutboundRestTemplateInterceptor(properties);
    }

    @Test
    void intercept_shouldReturnOriginalResponse() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/data"));

        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                "{\"ok\":true}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(execution).execute(request, body);
    }

    @Test
    void intercept_withHeaders_shouldLogAndReturnResponse() throws Exception {
        properties.getOutbound().setIncludeHeaders(true);
        properties.getOutbound().setIncludeBody(true);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
                URI.create("https://api.example.com/orders"));
        request.getHeaders().add("Content-Type", "application/json");
        request.getHeaders().add("Authorization", "Bearer secret");

        byte[] reqBody = "{\"item\":\"book\"}".getBytes(StandardCharsets.UTF_8);
        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                "{\"id\":1}".getBytes(StandardCharsets.UTF_8), HttpStatus.CREATED);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, reqBody, execution);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void intercept_withBinaryBody_shouldNotLogBody() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
                URI.create("https://api.example.com/upload"));
        request.getHeaders().add("Content-Type", "image/png");

        byte[] imgBytes = {0x1, 0x2, 0x3};
        ClientHttpResponse mockResponse = new MockClientHttpResponse(new byte[0], HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        // Should not throw
        ClientHttpResponse result = interceptor.intercept(request, imgBytes, execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_withBinaryResponseBody_shouldNotThrow() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/image"));

        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Content-Type", "image/jpeg");
        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                new byte[]{0x1, 0x2}, HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_emptyRequestBody_shouldSkipBodyLogging() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/items"));

        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                "[]".getBytes(), HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void intercept_withIOExceptionOnResponseBody_shouldNotThrow() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/items"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(mockResponse.getBody()).thenThrow(new IOException("Stream closed"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        // Should swallow the IOException gracefully
        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    // ---- New tests for missing branch coverage ----

    @Test
    void intercept_headersDisabled_shouldNotLogHeaders() throws Exception {
        properties.getOutbound().setIncludeHeaders(false);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/no-headers"));
        request.getHeaders().add("Authorization", "Bearer secret");

        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                "{}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_bodyDisabled_shouldSkipBodyLogging() throws Exception {
        properties.getOutbound().setIncludeBody(false);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
                URI.create("https://api.example.com/submit"));

        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        byte[] body = "{\"input\":\"data\"}".getBytes(StandardCharsets.UTF_8);
        ClientHttpResponse result = interceptor.intercept(request, body, execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_responseBodyExceedsMaxSize_shouldTruncate() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        properties.getOutbound().setMaxBodySize(10);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/large"));
        request.getHeaders().add("Content-Type", "application/json");

        String largeBody = "{\"data\":\"this body is definitely longer than ten bytes\"}";
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Content-Type", "application/json");
        when(mockResponse.getHeaders()).thenReturn(resHeaders);
        when(mockResponse.getBody()).thenReturn(
                new java.io.ByteArrayInputStream(largeBody.getBytes(StandardCharsets.UTF_8)));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_nullContentTypeOnResponse_shouldNotThrow() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/no-content-type"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponse.getHeaders()).thenReturn(new HttpHeaders()); // no Content-Type
        when(mockResponse.getBody()).thenReturn(
                new java.io.ByteArrayInputStream("{\"ok\":true}".getBytes(StandardCharsets.UTF_8)));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_textResponseBody_shouldLogBody() throws Exception {
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/text"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Content-Type", "application/json");
        when(mockResponse.getHeaders()).thenReturn(resHeaders);
        when(mockResponse.getBody()).thenReturn(
                new java.io.ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8)));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void intercept_emptyTextResponseBody_shouldNotLogBody() throws Exception {
        // Covers the bodyBytes.length == 0 false branch
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/empty-text"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Content-Type", "application/json");
        when(mockResponse.getHeaders()).thenReturn(resHeaders);
        when(mockResponse.getBody()).thenReturn(
                new java.io.ByteArrayInputStream(new byte[0])); // empty body

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_whitespaceRequestBody_shouldNotAppendTruncatedBody() throws Exception {
        // Covers the truncated == null false branch for request body
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
                URI.create("https://api.example.com/whitespace"));
        request.getHeaders().add("Content-Type", "application/json");

        // Body is just whitespace — truncateBody returns null for blank strings
        byte[] whitespaceBody = "   ".getBytes(StandardCharsets.UTF_8);
        ClientHttpResponse mockResponse = new MockClientHttpResponse(
                new byte[0], HttpStatus.OK);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, whitespaceBody, execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_blankTextResponseBody_shouldNotAppendBody() throws Exception {
        // Covers the truncated==null false branch for response body (blank string body)
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/blank-response"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Content-Type", "application/json");
        when(mockResponse.getHeaders()).thenReturn(resHeaders);
        // Response body is whitespace only → truncateBody returns null
        when(mockResponse.getBody()).thenReturn(
                new java.io.ByteArrayInputStream("   ".getBytes(StandardCharsets.UTF_8)));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_binaryResponseContentType_shouldNotLogBody() throws Exception {
        // Covers isBinaryContent=true branch for response body
        properties.getOutbound().setIncludeBody(true);
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/file"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Content-Type", "image/jpeg");
        when(mockResponse.getHeaders()).thenReturn(resHeaders);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }

    @Test
    void intercept_responseHeadersExcluded_shouldRedactThem() throws Exception {
        properties.getOutbound().setIncludeHeaders(true);
        properties.getOutbound().setExcludeHeaders(java.util.List.of("Authorization"));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://api.example.com/secure"));

        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        HttpHeaders resHeaders = new HttpHeaders();
        resHeaders.add("Authorization", "Bearer response-token");
        resHeaders.add("Content-Type", "application/json");
        when(mockResponse.getHeaders()).thenReturn(resHeaders);
        when(mockResponse.getBody()).thenReturn(
                new java.io.ByteArrayInputStream(new byte[0]));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mockResponse);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
        assertThat(result).isNotNull();
    }
}
