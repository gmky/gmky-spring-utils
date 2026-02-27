package dev.gmky.utils.csv.callback;

import dev.gmky.utils.csv.config.CsvReaderConfig;
import dev.gmky.utils.csv.model.CsvReadResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingCsvReadCallbackTest {

    @Test
    void testAllLifecycleMethodsExecuteWithoutExceptions() {
        LoggingCsvReadCallback<Object> callback = new LoggingCsvReadCallback<>();
        CsvReaderConfig config = CsvReaderConfig.defaultConfig();
        
        // Assert that the logging doesn't throw exceptions, which covers the instructions.
        assertThatCode(() -> {
            callback.onStart(config);
            callback.onHeader(new String[]{"header1", "header2"});
            callback.onRow(1, new Object());
            callback.onError(1, "raw,line", new RuntimeException("test error"));
            callback.onComplete(new CsvReadResult<>(Collections.emptyList(), Collections.emptyList(), 0, 0, 0, Duration.ZERO));
            
            // Test default methods on interface itself
            CsvReadCallback<Object> defaultCallback = new CsvReadCallback<>() {};
            defaultCallback.onStart(config);
            defaultCallback.onHeader(new String[0]);
            defaultCallback.onRow(1, new Object());
            defaultCallback.onError(1, "raw", new RuntimeException());
            defaultCallback.onComplete(null);
        }).doesNotThrowAnyException();
    }
}
