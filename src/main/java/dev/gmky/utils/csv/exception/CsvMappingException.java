package dev.gmky.utils.csv.exception;

/**
 * Thrown when a CSV column value cannot be mapped to a DTO field
 * (e.g. type conversion failure, required field is missing).
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class CsvMappingException extends RuntimeException {

    private final long lineNumber;
    private final String fieldName;
    private final String rawValue;

    public CsvMappingException(long lineNumber, String fieldName, String rawValue, String message, Throwable cause) {
        super(String.format("CSV mapping failed at line %d, field [%s], value [%s]: %s",
                lineNumber, fieldName, rawValue, message), cause);
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
        this.rawValue = rawValue;
    }

    public CsvMappingException(long lineNumber, String fieldName, String rawValue, String message) {
        this(lineNumber, fieldName, rawValue, message, null);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getRawValue() {
        return rawValue;
    }
}
