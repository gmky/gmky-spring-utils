package dev.gmky.utils.csv.config;

import dev.gmky.utils.csv.converter.TypeConverterRegistry;
import dev.gmky.utils.csv.converter.impl.*;
import dev.gmky.utils.csv.reader.CsvReader;
import dev.gmky.utils.csv.validator.CsvRowValidator;
import dev.gmky.utils.csv.validator.JakartaValidationCsvRowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot Auto-configuration for CSV utilities.
 * <p>
 * Activated when {@link CsvReader} is on the classpath (i.e. when this library is included).
 * Registers the {@link TypeConverterRegistry} and a default {@link CsvRowValidator}.
 * Both beans can be overridden by user-defined beans.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(CsvReader.class)
public class CsvAutoConfiguration {

    /**
     * Registers all built-in type converters.
     * Override this bean to add or replace converters globally.
     */
    @Bean
    @ConditionalOnMissingBean
    public TypeConverterRegistry csvTypeConverterRegistry() {
        log.info("Initializing CsvTypeConverterRegistry with built-in converters...");
        TypeConverterRegistry registry = new TypeConverterRegistry();
        registry.register(new StringConverter());
        registry.register(new NumberConverter());
        registry.register(new BooleanConverter());
        registry.register(new TemporalConverter());
        registry.register(new EnumConverter());
        registry.register(new BigDecimalConverter());
        return registry;
    }

    /**
     * Registers the Jakarta Bean Validation-backed row validator.
     * Only registered if a Jakarta Validation provider (e.g. Hibernate Validator) is on the classpath.
     * Override this bean to use a custom validator or disable validation.
     */
    @Bean
    @ConditionalOnMissingBean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            name = "jakarta.validation.executable.ExecutableValidator"
    )
    public CsvRowValidator<?> csvRowValidator() {
        log.info("Initializing JakartaValidationCsvRowValidator...");
        return new JakartaValidationCsvRowValidator<>();
    }
}
