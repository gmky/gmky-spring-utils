package dev.gmky.utils.logging.http.interceptor;

import dev.gmky.utils.logging.http.config.HttpLoggingProperties;
import dev.gmky.utils.logging.http.util.HttpLoggingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link ClientHttpRequestInterceptor} that logs outbound HTTP requests and responses
 * made via Spring's {@code RestTemplate}.
 * <p>
 * Register on a {@code RestTemplate} via a {@code RestTemplateCustomizer} configured
 * in {@code HttpLoggingAutoConfiguration}. The factory must be wrapped in a
 * {@code BufferingClientHttpRequestFactory} so the response body can be read twice
 * (once for logging, once for the application).
 * </p>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@RequiredArgsConstructor
public class OutboundRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final HttpLoggingProperties properties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        HttpLoggingProperties.OutboundConfig cfg = properties.getOutbound();
        String level = cfg.getLogLevel();
        String uri = request.getURI().toString();

        // --- Log request ---
        StringBuilder reqLog = new StringBuilder();
        reqLog.append("\n>>> OUTBOUND [").append(request.getMethod()).append(" ").append(uri).append("]");

        if (cfg.isIncludeHeaders()) {
            reqLog.append("\n  Headers:")
                    .append(HttpLoggingHelper.formatHeaders(request.getHeaders(), cfg.getExcludeHeaders()));
        }

        if (cfg.isIncludeBody() && body.length > 0) {
            String contentType = request.getHeaders().getFirst("Content-Type");
            if (HttpLoggingHelper.isBinaryContent(contentType)) {
                reqLog.append("\n  Body: ").append(HttpLoggingHelper.binaryContentLabel());
            } else {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                String truncated = HttpLoggingHelper.truncateBody(bodyStr, cfg.getMaxBodySize());
                if (truncated != null) reqLog.append("\n  Body: ").append(truncated);
            }
        }

        HttpLoggingHelper.logAtLevel(log, level, reqLog.toString());

        // --- Execute ---
        long start = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long elapsed = System.currentTimeMillis() - start;

        // --- Log response ---
        StringBuilder resLog = new StringBuilder();
        resLog.append("\n<<< OUTBOUND [").append(request.getMethod()).append(" ").append(uri)
                .append("] -> ").append(response.getStatusCode())
                .append(" (").append(elapsed).append("ms)");

        if (cfg.isIncludeHeaders()) {
            resLog.append("\n  Headers:")
                    .append(HttpLoggingHelper.formatHeaders(response.getHeaders(), cfg.getExcludeHeaders()));
        }

        if (cfg.isIncludeBody()) {
            // Response body is readable only if BufferingClientHttpRequestFactory was used
            String contentType = response.getHeaders().getFirst("Content-Type");
            if (HttpLoggingHelper.isBinaryContent(contentType)) {
                resLog.append("\n  Body: ").append(HttpLoggingHelper.binaryContentLabel());
            } else {
                try {
                    byte[] bodyBytes = response.getBody().readAllBytes();
                    if (bodyBytes.length > 0) {
                        String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
                        String truncated = HttpLoggingHelper.truncateBody(bodyStr, cfg.getMaxBodySize());
                        if (truncated != null) resLog.append("\n  Body: ").append(truncated);
                    }
                } catch (Exception e) {
                    log.trace("Could not read outbound response body for logging: {}", e.getMessage());
                }
            }
        }

        HttpLoggingHelper.logAtLevel(log, level, resLog.toString());
        return response;
    }
}
