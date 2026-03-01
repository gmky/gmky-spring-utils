package dev.gmky.utils.logging.http.config;

import dev.gmky.utils.logging.http.filter.InboundHttpLoggingFilter;
import dev.gmky.utils.logging.http.interceptor.OutboundRestTemplateInterceptor;
import dev.gmky.utils.logging.http.webclient.OutboundWebClientFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HttpLoggingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    HttpLoggingAutoConfiguration.class,
                    InboundHttpLoggingAutoConfiguration.class,
                    RestTemplateLoggingAutoConfiguration.class,
                    WebClientLoggingAutoConfiguration.class));

    @Test
    void whenDisabledByDefault_shouldRegisterNoBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(InboundHttpLoggingFilter.class);
            assertThat(ctx).doesNotHaveBean(OutboundRestTemplateInterceptor.class);
            assertThat(ctx).doesNotHaveBean(OutboundWebClientFilter.class);
        });
    }

    @Test
    void whenEnabled_shouldRegisterInboundFilter() {
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(InboundHttpLoggingFilter.class);
                    assertThat(ctx).hasSingleBean(HttpLoggingProperties.class);
                });
    }

    @Test
    void whenEnabled_shouldRegisterOutboundInterceptor() {
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(OutboundRestTemplateInterceptor.class));
    }

    @Test
    void whenEnabled_shouldRegisterWebClientFilter() {
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(OutboundWebClientFilter.class));
    }

    @Test
    void whenInboundDisabled_shouldNotRegisterInboundFilter() {
        contextRunner
                .withPropertyValues(
                        "gmky.logging.http.enabled=true",
                        "gmky.logging.http.inbound.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(InboundHttpLoggingFilter.class));
    }

    @Test
    void whenOutboundDisabled_shouldNotRegisterOutboundBeans() {
        contextRunner
                .withPropertyValues(
                        "gmky.logging.http.enabled=true",
                        "gmky.logging.http.outbound.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboundRestTemplateInterceptor.class);
                    assertThat(ctx).doesNotHaveBean(OutboundWebClientFilter.class);
                });
    }

    @Test
    void properties_shouldBindFromPropertyValues() {
        contextRunner
                .withPropertyValues(
                        "gmky.logging.http.enabled=true",
                        "gmky.logging.http.inbound.include-body=true",
                        "gmky.logging.http.inbound.max-body-size=1024",
                        "gmky.logging.http.inbound.log-level=INFO",
                        "gmky.logging.http.outbound.log-level=WARN")
                .run(ctx -> {
                    HttpLoggingProperties props = ctx.getBean(HttpLoggingProperties.class);
                    assertThat(props.getInbound().isIncludeBody()).isTrue();
                    assertThat(props.getInbound().getMaxBodySize()).isEqualTo(1024);
                    assertThat(props.getInbound().getLogLevel()).isEqualTo("INFO");
                    assertThat(props.getOutbound().getLogLevel()).isEqualTo("WARN");
                });
    }

    @Test
    void whenEnabled_shouldRegisterRestTemplateCustomizer() {
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> assertThat(ctx).hasBean("httpLoggingRestTemplateCustomizer"));
    }

    @Test
    void restTemplateCustomizer_shouldAddInterceptorAndWrapFactory() {
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> {
                    org.springframework.boot.web.client.RestTemplateCustomizer customizer =
                            ctx.getBean("httpLoggingRestTemplateCustomizer",
                                    org.springframework.boot.web.client.RestTemplateCustomizer.class);
                    org.springframework.web.client.RestTemplate restTemplate =
                            new org.springframework.web.client.RestTemplate();

                    customizer.customize(restTemplate);

                    // Interceptor should have been added
                    assertThat(restTemplate.getInterceptors()).hasSize(1);
                    assertThat(restTemplate.getInterceptors().get(0))
                            .isInstanceOf(
                                    dev.gmky.utils.logging.http.interceptor.OutboundRestTemplateInterceptor.class);
                });
    }

    @Test
    void restTemplateCustomizer_whenFactoryIsNull_shouldCreateSimpleFactory() {
        // Covers the defensive null branch: getRequestFactory() != null → false
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> {
                    org.springframework.boot.web.client.RestTemplateCustomizer customizer =
                            ctx.getBean("httpLoggingRestTemplateCustomizer",
                                    org.springframework.boot.web.client.RestTemplateCustomizer.class);

                    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
                    RestTemplate mockTemplate = mock(RestTemplate.class);
                    when(mockTemplate.getRequestFactory()).thenReturn(null); // null factory
                    when(mockTemplate.getInterceptors()).thenReturn(interceptors);
                    doNothing().when(mockTemplate).setRequestFactory(any());

                    customizer.customize(mockTemplate);

                    // setRequestFactory called with a BufferingClientHttpRequestFactory
                    // wrapping a newly created SimpleClientHttpRequestFactory
                    ArgumentCaptor<ClientHttpRequestFactory> captor =
                            ArgumentCaptor.forClass(ClientHttpRequestFactory.class);
                    verify(mockTemplate).setRequestFactory(captor.capture());
                    assertThat(captor.getValue())
                            .isInstanceOf(BufferingClientHttpRequestFactory.class);
                    assertThat(interceptors).hasSize(1);
                });
    }

    @Test
    void restTemplateCustomizer_whenAlreadyBuffering_shouldNotRewrapButStillAddInterceptor() {
        contextRunner
                .withPropertyValues("gmky.logging.http.enabled=true")
                .run(ctx -> {
                    org.springframework.boot.web.client.RestTemplateCustomizer customizer =
                            ctx.getBean("httpLoggingRestTemplateCustomizer",
                                    org.springframework.boot.web.client.RestTemplateCustomizer.class);
                    org.springframework.web.client.RestTemplate restTemplate =
                            new org.springframework.web.client.RestTemplate();

                    // Pre-wrap with BufferingClientHttpRequestFactory
                    org.springframework.http.client.BufferingClientHttpRequestFactory bufferingFactory =
                            new org.springframework.http.client.BufferingClientHttpRequestFactory(
                                    new org.springframework.http.client.SimpleClientHttpRequestFactory());
                    restTemplate.setRequestFactory(bufferingFactory);

                    customizer.customize(restTemplate);

                    // Interceptor should still be added
                    assertThat(restTemplate.getInterceptors()).hasSize(1);
                });
    }
}
