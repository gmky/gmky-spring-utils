package dev.gmky.utils.logging.http.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for HTTP request/response logging.
 * <p>
 * Example YAML:
 * </p>
 * <pre>{@code
 * gmky:
 *   logging:
 *     http:
 *       enabled: true
 *       inbound:
 *         enabled: true
 *         include-headers: true
 *         include-body: false
 *         max-body-size: 4096
 *         exclude-paths:
 *           - /actuator/**
 *         exclude-headers:
 *           - Authorization
 *           - Cookie
 *         log-level: DEBUG
 *       outbound:
 *         enabled: true
 *         include-headers: true
 *         include-body: false
 *         max-body-size: 4096
 *         exclude-headers:
 *           - Authorization
 *         log-level: DEBUG
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Data
@ConfigurationProperties(prefix = "gmky.logging.http")
public class HttpLoggingProperties {

    /**
     * Master switch for HTTP logging. Defaults to {@code false}.
     */
    private boolean enabled = false;

    /**
     * Inbound (server-side) request/response logging configuration.
     */
    private InboundConfig inbound = new InboundConfig();

    /**
     * Outbound (client-side) request/response logging configuration.
     */
    private OutboundConfig outbound = new OutboundConfig();

    /**
     * Configuration for inbound HTTP traffic (requests arriving at this service).
     */
    @Data
    public static class InboundConfig {

        /** Whether to enable inbound logging. Defaults to {@code true}. */
        private boolean enabled = true;

        /** Whether to log request and response headers. Defaults to {@code true}. */
        private boolean includeHeaders = true;

        /** Whether to log request and response body. Defaults to {@code false} for safety. */
        private boolean includeBody = false;

        /** Maximum body size in bytes before truncation. Defaults to 4096. */
        private int maxBodySize = 4096;

        /**
         * Ant-style path patterns to exclude from logging.
         * Defaults to common infrastructure paths.
         */
        private List<String> excludePaths = List.of("/actuator/**", "/health", "/favicon.ico");

        /**
         * Header names to redact from logs (case-insensitive).
         * Defaults to sensitive auth headers.
         */
        private List<String> excludeHeaders = List.of("Authorization", "Cookie", "Set-Cookie");

        /** SLF4J log level for inbound logs: TRACE, DEBUG, INFO, WARN, ERROR. Defaults to DEBUG. */
        private String logLevel = "DEBUG";
    }

    /**
     * Configuration for outbound HTTP traffic (requests sent from this service to others).
     */
    @Data
    public static class OutboundConfig {

        /** Whether to enable outbound logging. Defaults to {@code true}. */
        private boolean enabled = true;

        /** Whether to log request and response headers. Defaults to {@code true}. */
        private boolean includeHeaders = true;

        /** Whether to log request and response body. Defaults to {@code false} for safety. */
        private boolean includeBody = false;

        /** Maximum body size in bytes before truncation. Defaults to 4096. */
        private int maxBodySize = 4096;

        /**
         * Header names to redact from logs (case-insensitive).
         */
        private List<String> excludeHeaders = List.of("Authorization", "Cookie");

        /** SLF4J log level for outbound logs: TRACE, DEBUG, INFO, WARN, ERROR. Defaults to DEBUG. */
        private String logLevel = "DEBUG";
    }
}
