package dev.gmky.utils.logging.http.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shared utility for HTTP logging: header masking, body truncation,
 * path exclusion matching, and dynamic log-level dispatch.
 *
 * @author HiepVH
 * @since 1.0.4
 */
public final class HttpLoggingHelper {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String REDACTED = "[REDACTED]";
    private static final String TRUNCATED_SUFFIX = "...[truncated]";
    private static final String BINARY_CONTENT = "[binary content]";

    private HttpLoggingHelper() {}

    /**
     * Formats HTTP headers into a human-readable string, redacting any header
     * whose name appears (case-insensitively) in {@code excludeList}.
     *
     * @param headers     the HTTP headers to format
     * @param excludeList header names to redact
     * @return formatted multi-line string of headers
     */
    public static String formatHeaders(HttpHeaders headers, List<String> excludeList) {
        if (headers == null || headers.isEmpty()) {
            return "(none)";
        }

        // Normalize excludeList to lowercase for case-insensitive comparison
        List<String> lowerExcludes = excludeList == null ? List.of()
                : excludeList.stream().map(String::toLowerCase).toList();

        // Populate a case-insensitive sorted map via HttpHeaders.forEach().
        // NOTE: DO NOT use TreeMap.putAll(headers) — in Spring Framework 7+,
        // HttpHeaders no longer implements java.util.Map, so putAll() throws
        // IncompatibleClassChangeError at runtime even though it compiled fine
        // against Spring 5/6 where HttpHeaders did implement Map.
        Map<String, List<String>> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((name, values) ->
                sorted.put(name, new ArrayList<>(values)));

        StringBuilder sb = new StringBuilder();
        sorted.forEach((name, values) -> {
            String value = lowerExcludes.contains(name.toLowerCase())
                    ? REDACTED
                    : String.join(", ", values);
            sb.append("\n  ").append(name).append(": ").append(value);
        });
        return sb.toString();
    }

    /**
     * Truncates a body string to {@code maxSize} bytes. Returns {@code null}
     * if the input is blank.
     *
     * @param body    the raw body string
     * @param maxSize maximum byte length to retain
     * @return truncated body or {@code null} if blank
     */
    public static String truncateBody(String body, int maxSize) {
        if (StringUtils.isBlank(body)) return null;
        byte[] bytes = body.getBytes();
        if (bytes.length <= maxSize) return body;
        return new String(bytes, 0, maxSize) + TRUNCATED_SUFFIX;
    }

    /**
     * Returns {@code true} if the given URI matches any of the provided
     * Ant-style path patterns.
     *
     * @param uri      the request URI
     * @param patterns list of Ant patterns (e.g. {@code /actuator/**})
     * @return {@code true} if excluded
     */
    public static boolean matchesExcludePath(String uri, List<String> patterns) {
        if (uri == null || patterns == null || patterns.isEmpty()) return false;
        return patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }

    /**
     * Returns {@code true} if the Content-Type represents binary or multipart content
     * that should not be printed as text.
     *
     * @param contentType the Content-Type header value (may be null)
     * @return {@code true} if binary
     */
    public static boolean isBinaryContent(String contentType) {
        if (contentType == null) return false;
        String lower = contentType.toLowerCase();
        return lower.startsWith("image/")
                || lower.startsWith("audio/")
                || lower.startsWith("video/")
                || lower.contains("application/octet-stream")
                || lower.contains("multipart/");
    }

    /**
     * Placeholder label used when content should not be printed.
     *
     * @return the binary-content label string
     */
    public static String binaryContentLabel() {
        return BINARY_CONTENT;
    }

    /**
     * Dispatches a log message at the configured SLF4J level.
     *
     * @param logger  the SLF4J logger instance
     * @param level   one of TRACE, DEBUG, INFO, WARN, ERROR (case-insensitive)
     * @param message the message format string
     * @param args    the message arguments
     */
    public static void logAtLevel(Logger logger, String level, String message, Object... args) {
        if (level == null) {
            logger.debug(message, args);
            return;
        }
        switch (level.toUpperCase()) {
            case "TRACE" -> logger.trace(message, args);
            case "INFO"  -> logger.info(message, args);
            case "WARN"  -> logger.warn(message, args);
            case "ERROR" -> logger.error(message, args);
            default      -> logger.debug(message, args);
        }
    }
}
