package dev.gmky.utils.batch.core.processor;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CompositeProcessor.
 */
class CompositeProcessorTest {

    @Test
    void testSingleProcessor() throws Exception {
        ItemProcessor<String, String> processor = String::toUpperCase;

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(processor);

        String result = composite.process("test");

        assertEquals("TEST", result);
    }

    @Test
    void testMultipleProcessors() throws Exception {
        ItemProcessor<String, String> uppercase = String::toUpperCase;
        ItemProcessor<String, String> addPrefix = item -> "PREFIX_" + item;
        ItemProcessor<String, String> addSuffix = item -> item + "_SUFFIX";

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(
            uppercase,
            addPrefix,
            addSuffix
        );

        String result = composite.process("test");

        assertEquals("PREFIX_TEST_SUFFIX", result);
    }

    @Test
    void testProcessorReturnsNull() throws Exception {
        ItemProcessor<String, String> alwaysNull = item -> null;
        ItemProcessor<String, String> neverCalled = item -> {
            fail("Should not be called");
            return item;
        };

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(
            alwaysNull,
            neverCalled
        );

        String result = composite.process("test");

        assertNull(result);
    }

    @Test
    void testProcessorChainWithFiltering() throws Exception {
        ItemProcessor<String, String> filterShort = item ->
            item.length() < 3 ? null : item;
        ItemProcessor<String, String> uppercase = String::toUpperCase;

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(
            filterShort,
            uppercase
        );

        assertNull(composite.process("ab"));
        assertEquals("TEST", composite.process("test"));
    }

    @Test
    void testTypeTransformation() throws Exception {
        ItemProcessor<String, Integer> parseInteger = Integer::parseInt;
        ItemProcessor<Integer, String> multiplyAndFormat = num ->
            "Result: " + (num * 2);

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(
            parseInteger,
            multiplyAndFormat
        );

        String result = composite.process("5");

        assertEquals("Result: 10", result);
    }

    @Test
    void testListConstructor() throws Exception {
        List<ItemProcessor<?, ?>> processors = new ArrayList<>();
        processors.add((ItemProcessor<String, String>) String::toUpperCase);
        processors.add((ItemProcessor<String, String>) item -> item + "!");

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(processors);

        String result = composite.process("hello");

        assertEquals("HELLO!", result);
    }

    @Test
    void testGetProcessors() {
        ItemProcessor<String, String> p1 = item -> item;
        ItemProcessor<String, String> p2 = item -> item;

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(p1, p2);

        List<ItemProcessor<?, ?>> processors = composite.getProcessors();

        assertEquals(2, processors.size());
        // Verify it returns a copy, not the original list
        processors.clear();
        assertEquals(2, composite.getProcessors().size());
    }

    @Test
    void testEmptyProcessorChain() throws Exception {
        CompositeProcessor<String, String> composite = new CompositeProcessor<>();

        String result = composite.process("test");

        assertEquals("test", result);
    }

    @Test
    void testProcessorThrowsException() {
        ItemProcessor<String, String> throwingProcessor = item -> {
            throw new RuntimeException("Processing failed");
        };

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(throwingProcessor);

        assertThrows(RuntimeException.class, () -> composite.process("test"));
    }

    @Test
    void testComplexChain() throws Exception {
        // Simulate: trim -> validate -> transform -> enrich
        ItemProcessor<String, String> trim = String::trim;
        ItemProcessor<String, String> validate = item ->
            item.isEmpty() ? null : item;
        ItemProcessor<String, String> transform = String::toUpperCase;
        ItemProcessor<String, String> enrich = item ->
            "[" + item + "]";

        CompositeProcessor<String, String> composite = new CompositeProcessor<>(
            trim,
            validate,
            transform,
            enrich
        );

        assertEquals("[HELLO]", composite.process("  hello  "));
        assertNull(composite.process("   "));
    }
}
