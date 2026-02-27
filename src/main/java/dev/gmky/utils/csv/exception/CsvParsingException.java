package dev.gmky.utils.csv.exception;

/**
 * Thrown when a CSV row cannot be tokenized (e.g. malformed quoting or structural errors).
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class CsvParsingException extends RuntimeException {

    private final long lineNumber;
    private final String rawLine;

    public CsvParsingException(long lineNumber, String rawLine, String message, Throwable cause) {
        super(String.format("CSV parsing failed at line %d: %s. Raw: [%s]", lineNumber, message, rawLine), cause);
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
    }

    public CsvParsingException(long lineNumber, String rawLine, String message) {
        this(lineNumber, rawLine, message, null);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getRawLine() {
        return rawLine;
    }
}
