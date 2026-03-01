package dev.gmky.utils;

import dev.gmky.utils.common.AppContextUtil;
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
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Main auto-configuration class for GMKY Spring Utils library.
 * <p>
 * Registers all core utility beans explicitly. No component scanning is used —
 * this follows Spring Boot library best practices to avoid interfering with
 * consumer application component scan configurations.
 * </p>
 *
 * @author HiepVH
 * @see org.springframework.boot.autoconfigure.AutoConfiguration
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@PropertySource("classpath:application.properties")
public class GmkyAutoConfiguration {

    private final Environment env;

    @Bean
    @ConditionalOnMissingBean(AppReadyLogging.class)
    public AppReadyLogging appReadyLogging() {
        log.debug("Initializing AppReadyLogging...");
        return new AppReadyLoggingImpl(env);
    }

    @Bean
    @ConditionalOnMissingBean(ExecutionTimeAspect.class)
    public ExecutionTimeAspectImpl executionTimeAspect() {
        log.debug("Initializing ExecutionTimeAspect...");
        return new ExecutionTimeAspectImpl();
    }

    @Bean
    @ConditionalOnMissingBean(LogPrefixAspect.class)
    public LogPrefixAspectImpl logPrefixAspect() {
        log.debug("Initializing LogPrefixAspect...");
        return new LogPrefixAspectImpl();
    }

    @Bean
    @ConditionalOnMissingBean(AppContextUtil.class)
    public AppContextUtil appContextUtil() {
        log.debug("Initializing AppContextUtil...");
        return new AppContextUtil();
    }
}
