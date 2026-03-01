package dev.gmky.utils.logging.http.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Master auto-configuration for HTTP logging.
 * <p>
 * This class only binds {@link HttpLoggingProperties} — it contains <strong>no
 * {@code @Bean} methods</strong> that reference optional types such as
 * {@code OncePerRequestFilter}, {@code RestTemplateCustomizer}, or
 * {@code WebClientCustomizer}.
 * </p>
 * <p>
 * Each optional-dependency feature lives in its own top-level
 * {@code @AutoConfiguration} class:
 * <ul>
 *   <li>{@link InboundHttpLoggingAutoConfiguration} — {@code spring-web} / Servlet</li>
 *   <li>{@link RestTemplateLoggingAutoConfiguration} — {@code RestTemplate}</li>
 *   <li>{@link WebClientLoggingAutoConfiguration} — {@code spring-webflux}</li>
 * </ul>
 * Separating them prevents Spring Framework 7+'s strict
 * {@link org.springframework.util.ReflectionUtils#getDeclaredMethods} from
 * triggering {@link NoClassDefFoundError} when it introspects this class while
 * evaluating {@code @ConditionalOnMissingBean} for other beans.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(HttpLoggingProperties.class)
@ConditionalOnProperty(prefix = "gmky.logging.http", name = "enabled", havingValue = "true")
public class HttpLoggingAutoConfiguration {
    // Intentionally empty — see Javadoc above.
    // All beans are in sibling AutoConfiguration classes guarded by @ConditionalOnClass.
}
