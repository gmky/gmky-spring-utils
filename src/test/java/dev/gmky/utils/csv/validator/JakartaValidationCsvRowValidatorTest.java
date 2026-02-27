package dev.gmky.utils.csv.validator;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaValidationCsvRowValidatorTest {

    static class ValidDto {
        @NotBlank
        private String name;

        public ValidDto(String name) {
            this.name = name;
        }
    }

    @Test
    void testValidation() {
        JakartaValidationCsvRowValidator<ValidDto> validator = new JakartaValidationCsvRowValidator<>();

        // Valid
        ValidDto valid = new ValidDto("John");
        List<String> violations = validator.validate(valid);
        assertThat(violations).isEmpty();

        // Invalid
        ValidDto invalid = new ValidDto("");
        List<String> invalidViolations = validator.validate(invalid);
        assertThat(invalidViolations).hasSize(1);
        assertThat(invalidViolations.get(0)).contains("name:");
    }
}
