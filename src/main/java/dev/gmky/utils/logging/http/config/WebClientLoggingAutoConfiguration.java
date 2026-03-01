package dev.gmky.utils.logging.http.config;

import dev.gmky.utils.logging.http.webclient.OutboundWebClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for outbound WebClient HTTP logging.
 * <p>
 * <strong>Classpath guard:</strong> {@code @ConditionalOnClass(WebClient.class)} at
 * class level ensures the JVM only loads this class — and resolves all return types
 * of its {@code @Bean} methods — when {@code WebClient} is actually present.
 * </p>
 * <p>
 * <strong>Note on {@code WebClientCustomizer}:</strong> This class intentionally does
 * NOT declare a {@code WebClientCustomizer} bean, because {@code WebClientCustomizer}
 * lives in {@code spring-boot-autoconfigure} and its availability cannot be guaranteed
 * independently of the consumer's Spring Boot version or dependency set. Consumers who
 * want auto-registration should add the filter manually:
 * </p>
 * <pre>{@code
 * @Bean
 * WebClient.Builder webClientBuilder(OutboundWebClientFilter filter) {
 *     return WebClient.builder().filter(filter);
 * }
 * }</pre>
 * <p>
 * Alternatively, consumers can declare their own {@code WebClientCustomizer} bean that
 * references the auto-configured {@link OutboundWebClientFilter}.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@AutoConfiguration(after = HttpLoggingAutoConfiguration.class)
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = "gmky.logging.http", name = "enabled", havingValue = "true")
public class WebClientLoggingAutoConfiguration {

    /**
     * Registers the {@link OutboundWebClientFilter} as a Spring bean.
     * Wire it into a {@code WebClient.Builder} either manually or via a
     * {@code WebClientCustomizer} in your application configuration.
     */
    @Bean
    @ConditionalOnMissingBean(OutboundWebClientFilter.class)
    @ConditionalOnProperty(prefix = "gmky.logging.http.outbound", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboundWebClientFilter outboundWebClientFilter(HttpLoggingProperties properties) {
        log.info("Registering OutboundWebClientFilter");
        return new OutboundWebClientFilter(properties);
    }
}
