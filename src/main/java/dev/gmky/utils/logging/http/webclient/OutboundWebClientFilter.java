package dev.gmky.utils.logging.http.webclient;

import dev.gmky.utils.logging.http.config.HttpLoggingProperties;
import dev.gmky.utils.logging.http.util.HttpLoggingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * {@link ExchangeFilterFunction} that logs outbound HTTP traffic for WebClient.
 * <p>
 * Logs request method, URI, and headers before delegation.
 * Logs response status, headers, and latency after the response arrives.
 * </p>
 * <p>
 * <strong>Note:</strong> Response body logging is intentionally omitted for the reactive
 * pipeline — reading a {@code Flux<DataBuffer>} requires buffering the full payload which
 * risks blocking and defeats the purpose of reactive streaming. Enable
 * {@code include-headers} for structured metadata visibility instead.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.4
 */
@Slf4j
@RequiredArgsConstructor
public class OutboundWebClientFilter implements ExchangeFilterFunction {

    private final HttpLoggingProperties properties;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        HttpLoggingProperties.OutboundConfig cfg = properties.getOutbound();
        String level = cfg.getLogLevel();
        String uri = request.url().toString();

        // Log request
        StringBuilder reqLog = new StringBuilder();
        reqLog.append("\n>>> OUTBOUND (WebClient) [")
                .append(request.method()).append(" ").append(uri).append("]");

        if (cfg.isIncludeHeaders()) {
            reqLog.append("\n  Headers:")
                    .append(HttpLoggingHelper.formatHeaders(request.headers(), cfg.getExcludeHeaders()));
        }

        HttpLoggingHelper.logAtLevel(log, level, reqLog.toString());

        long start = System.currentTimeMillis();
        return next.exchange(request)
                .doOnNext(response -> {
                    long elapsed = System.currentTimeMillis() - start;
                    StringBuilder resLog = new StringBuilder();
                    resLog.append("\n<<< OUTBOUND (WebClient) [")
                            .append(request.method()).append(" ").append(uri)
                            .append("] -> ").append(response.statusCode())
                            .append(" (").append(elapsed).append("ms)");

                    if (cfg.isIncludeHeaders()) {
                        resLog.append("\n  Headers:")
                                .append(HttpLoggingHelper.formatHeaders(
                                        response.headers().asHttpHeaders(), cfg.getExcludeHeaders()));
                    }

                    HttpLoggingHelper.logAtLevel(log, level, resLog.toString());
                })
                .doOnError(err -> {
                    long elapsed = System.currentTimeMillis() - start;
                    HttpLoggingHelper.logAtLevel(log, "WARN",
                            "<<< OUTBOUND (WebClient) [{} {}] -> ERROR after {}ms: {}",
                            request.method(), uri, elapsed, err.getMessage());
                });
    }
}
