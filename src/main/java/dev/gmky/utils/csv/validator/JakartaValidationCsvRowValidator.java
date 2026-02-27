package dev.gmky.utils.csv.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link CsvRowValidator} implementation backed by Jakarta Bean Validation.
 * <p>
 * Uses standard annotations like {@code @NotNull}, {@code @NotBlank}, {@code @Size}, etc.
 * on the DTO class to validate each mapped row.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @Data
 * public class UserDto {
 *     @CsvColumn(value = "Email", required = true)
 *     @NotBlank
 *     @Email
 *     private String email;
 * }
 * }</pre>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
public class JakartaValidationCsvRowValidator<T> implements CsvRowValidator<T> {

    private final Validator validator;

    public JakartaValidationCsvRowValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Override
    public List<String> validate(T record) {
        Set<ConstraintViolation<T>> violations = validator.validate(record);
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());
    }
}
