package dev.gmky.utils.logging.http.config;

import dev.gmky.utils.logging.http.filter.InboundHttpLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Auto-configuration for inbound (server-side) HTTP request/response logging.
 * <p>
 * Guarded by {@code @ConditionalOnClass(OncePerRequestFilter.class)} at class level so
 * the JVM never attempts to resolve {@link InboundHttpLoggingFilter} or
 * {@link OncePerRequestFilter} when {@code spring-boot-starter-web} is absent.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@AutoConfiguration(after = HttpLoggingAutoConfiguration.class)
@ConditionalOnClass(OncePerRequestFilter.class)
@ConditionalOnProperty(prefix = "gmky.logging.http", name = "enabled", havingValue = "true")
public class InboundHttpLoggingAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @ConditionalOnMissingBean(InboundHttpLoggingFilter.class)
    @ConditionalOnProperty(prefix = "gmky.logging.http.inbound", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public InboundHttpLoggingFilter inboundHttpLoggingFilter(HttpLoggingProperties properties) {
        log.info("Registering InboundHttpLoggingFilter (include-body={}, include-headers={})",
                properties.getInbound().isIncludeBody(),
                properties.getInbound().isIncludeHeaders());
        return new InboundHttpLoggingFilter(properties);
    }
}
