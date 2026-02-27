package dev.gmky.utils.csv.model;

/**
 * Represents a single row-level error that occurred during CSV reading.
 *
 * @author HiepVH
 * @since 1.0.3
 */
public record CsvError(
    long lineNumber,
    String rawLine,
    String fieldName,
    String message,
    Exception cause
) {
    /**
     * Creates an error without a specific field name (e.g. for parse-level errors).
     */
    public static CsvError of(long lineNumber, String rawLine, String message, Exception cause) {
        return new CsvError(lineNumber, rawLine, null, message, cause);
    }
}
