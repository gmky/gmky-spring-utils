package dev.gmky.utils.logging.http.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLoggingPropertiesTest {

    @Test
    void defaultValues_shouldBeConservative() {
        HttpLoggingProperties props = new HttpLoggingProperties();

        assertThat(props.isEnabled()).isFalse();

        HttpLoggingProperties.InboundConfig inbound = props.getInbound();
        assertThat(inbound.isEnabled()).isTrue();
        assertThat(inbound.isIncludeHeaders()).isTrue();
        assertThat(inbound.isIncludeBody()).isFalse();
        assertThat(inbound.getMaxBodySize()).isEqualTo(4096);
        assertThat(inbound.getLogLevel()).isEqualToIgnoringCase("DEBUG");
        assertThat(inbound.getExcludePaths()).isNotEmpty();
        assertThat(inbound.getExcludeHeaders()).contains("Authorization", "Cookie");

        HttpLoggingProperties.OutboundConfig outbound = props.getOutbound();
        assertThat(outbound.isEnabled()).isTrue();
        assertThat(outbound.isIncludeHeaders()).isTrue();
        assertThat(outbound.isIncludeBody()).isFalse();
        assertThat(outbound.getMaxBodySize()).isEqualTo(4096);
        assertThat(outbound.getLogLevel()).isEqualToIgnoringCase("DEBUG");
        assertThat(outbound.getExcludeHeaders()).contains("Authorization");
    }

    @Test
    void mutators_shouldUpdateValues() {
        HttpLoggingProperties props = new HttpLoggingProperties();
        props.setEnabled(true);
        props.getInbound().setIncludeBody(true);
        props.getInbound().setMaxBodySize(1024);
        props.getInbound().setExcludePaths(List.of("/skip/**"));
        props.getOutbound().setLogLevel("INFO");

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getInbound().isIncludeBody()).isTrue();
        assertThat(props.getInbound().getMaxBodySize()).isEqualTo(1024);
        assertThat(props.getInbound().getExcludePaths()).containsExactly("/skip/**");
        assertThat(props.getOutbound().getLogLevel()).isEqualTo("INFO");
    }
}
