package dev.gmky.utils.csv.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CsvAutoConfiguration}.
 */
class CsvAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CsvAutoConfiguration.class));

    @Test
    void shouldRegisterTypeConverterRegistryBean() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(
                dev.gmky.utils.csv.converter.TypeConverterRegistry.class));
    }

    @Test
    void shouldRegisterCsvRowValidatorBean() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(
                dev.gmky.utils.csv.validator.CsvRowValidator.class));
    }

    @Test
    void shouldAllowUserToOverrideTypeConverterRegistry() {
        runner.withUserConfiguration(CustomRegistryConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(dev.gmky.utils.csv.converter.TypeConverterRegistry.class);
            assertThat(ctx).hasBean("customRegistry");
        });
    }

    @org.springframework.context.annotation.Configuration
    static class CustomRegistryConfig {
        @org.springframework.context.annotation.Bean("customRegistry")
        public dev.gmky.utils.csv.converter.TypeConverterRegistry customRegistry() {
            return new dev.gmky.utils.csv.converter.TypeConverterRegistry();
        }
    }
}
