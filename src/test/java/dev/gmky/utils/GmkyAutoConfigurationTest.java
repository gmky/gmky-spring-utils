package dev.gmky.utils;

import dev.gmky.utils.execution.aop.ExecutionTimeAspect;
import dev.gmky.utils.execution.aop.ExecutionTimeAspectImpl;
import dev.gmky.utils.logging.aop.LogPrefixAspect;
import dev.gmky.utils.logging.aop.LogPrefixAspectImpl;
import dev.gmky.utils.startup.AppReadyLogging;
import dev.gmky.utils.startup.AppReadyLoggingImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GmkyAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GmkyAutoConfiguration.class));

    @Test
    void testAutoConfigurationCreatesAllBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AppReadyLogging.class);
            assertThat(context).hasSingleBean(ExecutionTimeAspect.class);
            assertThat(context).hasSingleBean(LogPrefixAspect.class);
        });
    }

    @Test
    void testAppReadyLoggingNotCreatedWhenUserProvides() {
        contextRunner.withUserConfiguration(CustomAppReadyLoggingConfig.class).run(context -> {
            assertThat(context).hasSingleBean(AppReadyLogging.class);
            assertThat(context).doesNotHaveBean(AppReadyLoggingImpl.class);
            assertThat(context).hasBean("customAppReadyLogging");
        });
    }

    @Test
    void testExecutionTimeAspectNotCreatedWhenUserProvides() {
        contextRunner.withUserConfiguration(CustomExecutionTimeAspectConfig.class).run(context -> {
            assertThat(context).hasSingleBean(ExecutionTimeAspect.class);
            assertThat(context).doesNotHaveBean(ExecutionTimeAspectImpl.class);
            assertThat(context).hasBean("customExecutionTimeAspect");
        });
    }

    @Test
    void testLogPrefixAspectNotCreatedWhenUserProvides() {
        contextRunner.withUserConfiguration(CustomLogPrefixAspectConfig.class).run(context -> {
            assertThat(context).hasSingleBean(LogPrefixAspect.class);
            assertThat(context).doesNotHaveBean(LogPrefixAspectImpl.class);
            assertThat(context).hasBean("customLogPrefixAspect");
        });
    }

    @Configuration
    static class CustomAppReadyLoggingConfig {
        @Bean
        public AppReadyLogging customAppReadyLogging() {
            return () -> {};
        }
    }

    @Configuration
    static class CustomExecutionTimeAspectConfig {
        @Bean
        public ExecutionTimeAspect customExecutionTimeAspect() {
            return joinPoint -> joinPoint.proceed();
        }
    }

    @Configuration
    static class CustomLogPrefixAspectConfig {
        @Bean
        public LogPrefixAspect customLogPrefixAspect() {
            return joinPoint -> joinPoint.proceed();
        }
    }
}
