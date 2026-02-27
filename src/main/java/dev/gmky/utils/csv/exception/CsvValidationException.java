package dev.gmky.utils.csv.exception;

import java.util.List;

/**
 * Thrown when a mapped DTO row fails Jakarta Bean Validation or custom validation.
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class CsvValidationException extends RuntimeException {

    private final long lineNumber;
    private final List<String> violations;

    public CsvValidationException(long lineNumber, List<String> violations) {
        super(String.format("CSV validation failed at line %d: %s", lineNumber, violations));
        this.lineNumber = lineNumber;
        this.violations = List.copyOf(violations);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public List<String> getViolations() {
        return violations;
    }
}
