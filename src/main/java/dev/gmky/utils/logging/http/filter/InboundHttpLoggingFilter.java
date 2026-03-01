package dev.gmky.utils.logging.http.filter;

import dev.gmky.utils.logging.http.config.HttpLoggingProperties;
import dev.gmky.utils.logging.http.util.HttpLoggingHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Servlet filter that logs inbound HTTP requests and responses.
 *
 * <p><strong>Request body logging:</strong> When {@code include-body=true}, the request
 * body is eagerly read into a byte array <em>before</em> the filter chain is invoked,
 * so it appears in the log at the moment the request enters the application — not after
 * the controller has returned. A {@link CachedBodyHttpServletRequest} wrapper replays
 * the buffered bytes for downstream readers (controllers, argument resolvers, etc.) so
 * no data is lost.
 *
 * <p><strong>Response body logging:</strong> The response is wrapped in a
 * {@link ContentCachingResponseWrapper} which buffers the bytes written by the controller,
 * then copies them back to the real response after logging.
 *
 * <p>Paths matching any pattern in {@code gmky.logging.http.inbound.exclude-paths}
 * pass through without any logging overhead.
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@RequiredArgsConstructor
public class InboundHttpLoggingFilter extends OncePerRequestFilter {

    private final HttpLoggingProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpLoggingProperties.InboundConfig cfg = properties.getInbound();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUri = query != null ? uri + "?" + query : uri;

        // Skip excluded paths
        if (HttpLoggingHelper.matchesExcludePath(uri, cfg.getExcludePaths())) {
            filterChain.doFilter(request, response);
            return;
        }

        // ----------------------------------------------------------------
        // REQUEST: eagerly buffer body so we can log it now, then replay it
        // ----------------------------------------------------------------
        byte[] requestBodyBytes = new byte[0];
        HttpServletRequest chainRequest;

        if (cfg.isIncludeBody()) {
            // Read entire body upfront (respects maxBodySize for logging, but buffers fully
            // so the controller receives all bytes)
            requestBodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
            // Wrap so downstream can still read the body from a fresh ByteArrayInputStream
            chainRequest = new CachedBodyHttpServletRequest(request, requestBodyBytes);
        } else {
            chainRequest = request;
        }

        // Log the request line (including body, now available)
        logRequest(chainRequest, fullUri, requestBodyBytes, cfg);

        // ----------------------------------------------------------------
        // RESPONSE: wrap to capture body after chain returns
        // ----------------------------------------------------------------
        ContentCachingResponseWrapper wrappedResponse =
                (response instanceof ContentCachingResponseWrapper ccr) ? ccr
                        : new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(chainRequest, wrappedResponse);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            logResponse(request, wrappedResponse, fullUri, elapsed, cfg);
            // MUST restore the body so the client still receives it
            wrappedResponse.copyBodyToResponse();
        }
    }

    // -------------------------------------------------------------------------
    // Logging helpers
    // -------------------------------------------------------------------------

    private void logRequest(HttpServletRequest request, String fullUri,
                            byte[] bodyBytes,
                            HttpLoggingProperties.InboundConfig cfg) {
        HttpLoggingProperties.LogLevel level = cfg.getLogLevel();
        StringBuilder sb = new StringBuilder();
        sb.append("\n>>> INBOUND  [").append(request.getMethod()).append(" ").append(fullUri).append("]");

        if (cfg.isIncludeHeaders()) {
            HttpHeaders headers = buildRequestHeaders(request);
            sb.append("\n  Headers:").append(HttpLoggingHelper.formatHeaders(headers, cfg.getExcludeHeaders()));
        }

        if (cfg.isIncludeBody() && bodyBytes.length > 0) {
            String contentType = request.getContentType();
            if (HttpLoggingHelper.isBinaryContent(contentType)) {
                sb.append("\n  Request Body: ").append(HttpLoggingHelper.binaryContentLabel());
            } else {
                Charset charset = resolveCharset(request.getCharacterEncoding());
                String body = new String(bodyBytes, charset);
                String truncated = HttpLoggingHelper.truncateBody(body, cfg.getMaxBodySize());
                if (truncated != null) sb.append("\n  Request Body: ").append(truncated);
            }
        }

        HttpLoggingHelper.logAtLevel(log, level, sb.toString());
    }

    private void logResponse(HttpServletRequest request,
                             ContentCachingResponseWrapper response,
                             String fullUri,
                             long elapsed,
                             HttpLoggingProperties.InboundConfig cfg) {
        HttpLoggingProperties.LogLevel level = cfg.getLogLevel();
        StringBuilder sb = new StringBuilder();
        sb.append("\n<<< INBOUND  [")
                .append(request.getMethod()).append(" ").append(fullUri)
                .append("] -> ").append(response.getStatus())
                .append(" (").append(elapsed).append("ms)");

        if (cfg.isIncludeHeaders()) {
            HttpHeaders responseHeaders = buildResponseHeaders(response);
            sb.append("\n  Headers:").append(HttpLoggingHelper.formatHeaders(responseHeaders, cfg.getExcludeHeaders()));
        }

        if (cfg.isIncludeBody()) {
            byte[] resBody = response.getContentAsByteArray();
            if (resBody.length > 0) {
                String contentType = response.getContentType();
                if (HttpLoggingHelper.isBinaryContent(contentType)) {
                    sb.append("\n  Response Body: ").append(HttpLoggingHelper.binaryContentLabel());
                } else {
                    String body = new String(resBody, StandardCharsets.UTF_8);
                    String truncated = HttpLoggingHelper.truncateBody(body, cfg.getMaxBodySize());
                    if (truncated != null) sb.append("\n  Response Body: ").append(truncated);
                }
            }
        }

        HttpLoggingHelper.logAtLevel(log, level, sb.toString());
    }

    private static HttpHeaders buildRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames())
                .forEach(name -> Collections.list(request.getHeaders(name))
                        .forEach(value -> headers.add(name, value)));
        return headers;
    }

    private static HttpHeaders buildResponseHeaders(HttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        response.getHeaderNames()
                .forEach(name -> response.getHeaders(name)
                        .forEach(value -> headers.add(name, value)));
        return headers;
    }

    private static Charset resolveCharset(String encoding) {
        if (encoding == null) return StandardCharsets.UTF_8;
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    // -------------------------------------------------------------------------
    // Inner: re-readable request wrapper backed by a buffered byte array
    // -------------------------------------------------------------------------

    /**
     * {@link HttpServletRequestWrapper} that replays a pre-buffered body on every call
     * to {@link #getInputStream()} and {@link #getReader()}.
     *
     * <p>This is necessary because {@link jakarta.servlet.ServletInputStream} is a
     * one-shot stream — once consumed for logging, the downstream (e.g. Jackson) would
     * see an empty body without this wrapper.
     */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener rl) { /* no-op */ }
                @Override public int read() { return bais.read(); }
                @Override public int read(byte[] b, int off, int len) { return bais.read(b, off, len); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return cachedBody.length; }

        @Override
        public long getContentLengthLong() { return cachedBody.length; }
    }
}
