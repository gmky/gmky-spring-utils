package dev.gmky.utils;

import dev.gmky.utils.execution.aop.ExecutionTimeAspect;
import dev.gmky.utils.execution.aop.ExecutionTimeAspectImpl;
import dev.gmky.utils.logging.aop.LogPrefixAspect;
import dev.gmky.utils.logging.aop.LogPrefixAspectImpl;
import dev.gmky.utils.startup.AppReadyLogging;
import dev.gmky.utils.startup.AppReadyLoggingImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Main auto-configuration class for GMKY Spring Utils library.
 * <p>
 * This configuration class enables automatic component scanning for all utility
 * classes and configurations provided by the GMKY Spring Utils library. When this
 * library is included as a dependency in a Spring Boot application, this configuration
 * will be automatically loaded through Spring Boot's auto-configuration mechanism.
 * </p>
 * <p>
 * The following components are automatically registered:
 * <ul>
 *   <li>Execution time monitoring aspect ({@code ExecutionTimeAspectImpl})</li>
 *   <li>Application startup logging ({@code AppReadyLoggingImpl})</li>
 *   <li>Common utility classes ({@code AppContextUtil}, {@code DateUtil}, {@code RequestUtil}, {@code ResponseUtil})</li>
 *   <li>Entity mapper interface ({@code EntityMapper})</li>
 * </ul>
 * </p>
 *
 * @author HiepVH
 * @see org.springframework.boot.autoconfigure.AutoConfiguration
 * @since 1.0.0
 */
@Slf4j
@Configuration
@AutoConfiguration
@RequiredArgsConstructor
@ComponentScan(basePackages = "dev.gmky.utils")
@PropertySource("classpath:application.properties")
public class GmkyAutoConfiguration {
    private final Environment env;

    @Bean
    @ConditionalOnMissingBean(AppReadyLogging.class)
    public AppReadyLogging appReadyLogging() {
        log.info("Initializing AppReadyLogging...");
        return new AppReadyLoggingImpl(env);
    }

    @Bean
    @ConditionalOnMissingBean(ExecutionTimeAspect.class)
    public ExecutionTimeAspectImpl executionTimeAspect() {
        log.info("Initializing ExecutionTimeAspect...");
        return new ExecutionTimeAspectImpl();
    }

    @Bean
    @ConditionalOnMissingBean(LogPrefixAspect.class)
    public LogPrefixAspectImpl logPrefixAspect() {
        log.info("Initializing LogPrefixAspect...");
        return new LogPrefixAspectImpl();
    }
}
