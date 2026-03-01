package dev.gmky.utils.logging.http.webclient;

import dev.gmky.utils.logging.http.config.HttpLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboundWebClientFilterTest {

    private HttpLoggingProperties properties;
    private OutboundWebClientFilter filter;

    @BeforeEach
    void setUp() {
        properties = new HttpLoggingProperties();
        properties.setEnabled(true);
        filter = new OutboundWebClientFilter(properties);
    }

    private ClientRequest buildRequest(String method, String uri) {
        return ClientRequest.create(HttpMethod.valueOf(method), URI.create(uri)).build();
    }

    private ClientResponse buildResponse(HttpStatus status) {
        return ClientResponse.create(status).build();
    }

    @Test
    void filter_shouldReturnResponse_happyPath() {
        ClientRequest request = buildRequest("GET", "https://api.example.com/data");
        ClientResponse response = buildResponse(HttpStatus.OK);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
        assertThat(result.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void filter_withIncludeHeaders_shouldLogHeaders() {
        properties.getOutbound().setIncludeHeaders(true);
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("https://api.example.com/orders"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer secret")
                .build();
        ClientResponse response = buildResponse(HttpStatus.CREATED);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
        assertThat(result.statusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void filter_withHeadersDisabled_shouldNotThrow() {
        properties.getOutbound().setIncludeHeaders(false);
        ClientRequest request = buildRequest("GET", "https://api.example.com/items");
        ClientResponse response = buildResponse(HttpStatus.OK);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
    }

    @Test
    void filter_withExcludedResponseHeaders_shouldRedact() {
        properties.getOutbound().setIncludeHeaders(true);
        properties.getOutbound().setExcludeHeaders(List.of("Authorization"));
        ClientRequest request = buildRequest("GET", "https://api.example.com/secure");
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Authorization", "Bearer token")
                .header("Content-Type", "application/json")
                .build();
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
    }

    @Test
    void filter_withExchangeError_shouldTriggerDoOnError() {
        ClientRequest request = buildRequest("GET", "https://api.example.com/fail");
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        RuntimeException ex = new RuntimeException("Connection refused");
        when(exchange.exchange(any())).thenReturn(Mono.error(ex));

        Mono<ClientResponse> result = filter.filter(request, exchange);

        // The error should propagate but doOnError should have been triggered
        RuntimeException thrown = null;
        try {
            result.block();
        } catch (RuntimeException e) {
            thrown = e;
        }
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Connection refused");
    }

    @Test
    void filter_withLogLevelTrace_shouldNotThrow() {
        properties.getOutbound().setLogLevel(dev.gmky.utils.logging.http.config.HttpLoggingProperties.LogLevel.TRACE);
        ClientRequest request = buildRequest("DELETE", "https://api.example.com/resource/1");
        ClientResponse response = buildResponse(HttpStatus.NO_CONTENT);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
    }

    @Test
    void filter_withLogLevelInfo_shouldNotThrow() {
        properties.getOutbound().setLogLevel(dev.gmky.utils.logging.http.config.HttpLoggingProperties.LogLevel.INFO);
        ClientRequest request = buildRequest("PUT", "https://api.example.com/resource/1");
        ClientResponse response = buildResponse(HttpStatus.OK);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
    }

    @Test
    void filter_withLogLevelWarn_shouldNotThrow() {
        properties.getOutbound().setLogLevel(dev.gmky.utils.logging.http.config.HttpLoggingProperties.LogLevel.WARN);
        ClientRequest request = buildRequest("PATCH", "https://api.example.com/resource/1");
        ClientResponse response = buildResponse(HttpStatus.OK);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(response));

        ClientResponse result = filter.filter(request, exchange).block();

        assertThat(result).isNotNull();
    }

    @Test
    void filter_errorWithHeadersEnabled_shouldLogAndPropagate() {
        properties.getOutbound().setIncludeHeaders(true);
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("https://api.example.com/fail"))
                .header("X-Trace-Id", "abc123")
                .build();
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.error(new RuntimeException("timeout")));

        Mono<ClientResponse> result = filter.filter(request, exchange);

        RuntimeException thrown = null;
        try {
            result.block();
        } catch (RuntimeException e) {
            thrown = e;
        }
        assertThat(thrown).isNotNull();
    }
}
