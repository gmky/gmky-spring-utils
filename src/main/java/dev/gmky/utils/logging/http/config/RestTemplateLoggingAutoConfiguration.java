package dev.gmky.utils.logging.http.config;

import dev.gmky.utils.logging.http.interceptor.OutboundRestTemplateInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for outbound {@link RestTemplate} HTTP logging.
 * <p>
 * Guarded by {@code @ConditionalOnClass({RestTemplate.class, RestTemplateCustomizer.class})}
 * at class level so the JVM never resolves these types when
 * {@code spring-boot-starter-web} is absent.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@AutoConfiguration(after = HttpLoggingAutoConfiguration.class)
@ConditionalOnClass({RestTemplate.class, RestTemplateCustomizer.class})
@ConditionalOnProperty(prefix = "gmky.logging.http", name = "enabled", havingValue = "true")
public class RestTemplateLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OutboundRestTemplateInterceptor.class)
    @ConditionalOnProperty(prefix = "gmky.logging.http.outbound", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboundRestTemplateInterceptor outboundRestTemplateInterceptor(HttpLoggingProperties properties) {
        log.info("Registering OutboundRestTemplateInterceptor");
        return new OutboundRestTemplateInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "httpLoggingRestTemplateCustomizer")
    @ConditionalOnProperty(prefix = "gmky.logging.http.outbound", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RestTemplateCustomizer httpLoggingRestTemplateCustomizer(
            OutboundRestTemplateInterceptor interceptor) {
        return restTemplate -> {
            if (!(restTemplate.getRequestFactory() instanceof BufferingClientHttpRequestFactory)) {
                restTemplate.setRequestFactory(
                        new BufferingClientHttpRequestFactory(
                                restTemplate.getRequestFactory() != null
                                        ? restTemplate.getRequestFactory()
                                        : new SimpleClientHttpRequestFactory()));
            }
            restTemplate.getInterceptors().add(interceptor);
        };
    }
}
