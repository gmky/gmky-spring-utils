package dev.gmky.utils.batch.core.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbstractDynamicWriter.
 */
class AbstractDynamicWriterTest {

    private TestDynamicWriter writer;

    @BeforeEach
    void setUp() {
        writer = new TestDynamicWriter();
    }

    @Test
    void testWriteItems() throws Exception {
        List<String> items = Arrays.asList("Item1", "Item2", "Item3");
        Chunk<String> chunk = new Chunk<>(items);

        writer.write(chunk);

        assertEquals(3, writer.getWrittenItems().size());
        assertTrue(writer.getWrittenItems().containsAll(items));
    }

    @Test
    void testPreWriteHook() throws Exception {
        List<String> items = Arrays.asList("Item1");
        Chunk<String> chunk = new Chunk<>(items);

        writer.write(chunk);

        assertTrue(writer.isPreWriteCalled());
    }

    @Test
    void testPostWriteHook() throws Exception {
        List<String> items = Arrays.asList("Item1");
        Chunk<String> chunk = new Chunk<>(items);

        writer.write(chunk);

        assertTrue(writer.isPostWriteCalled());
    }

    @Test
    void testEmptyChunk() throws Exception {
        Chunk<String> emptyChunk = new Chunk<>(Collections.emptyList());

        writer.write(emptyChunk);

        assertTrue(writer.getWrittenItems().isEmpty());
        assertFalse(writer.isPreWriteCalled());
        assertFalse(writer.isPostWriteCalled());
    }

    @Test
    void testWriteError() {
        writer.setThrowError(true);
        List<String> items = Arrays.asList("Item1");
        Chunk<String> chunk = new Chunk<>(items);

        Exception exception = assertThrows(RuntimeException.class, () -> writer.write(chunk));

        assertEquals("Write failed", exception.getMessage());
        assertTrue(writer.isErrorHandled());
    }

    @Test
    void testWriteLargeChunk() throws Exception {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            items.add("Item" + i);
        }
        Chunk<String> chunk = new Chunk<>(items);

        writer.write(chunk);

        assertEquals(1000, writer.getWrittenItems().size());
    }

    @Test
    void testWriteOrderPreserved() throws Exception {
        List<String> items = Arrays.asList("First", "Second", "Third");
        Chunk<String> chunk = new Chunk<>(items);

        writer.write(chunk);

        List<String> written = writer.getWrittenItems();
        assertEquals("First", written.get(0));
        assertEquals("Second", written.get(1));
        assertEquals("Third", written.get(2));
    }

    @Test
    void testMultipleWrites() throws Exception {
        Chunk<String> chunk1 = new Chunk<>(Arrays.asList("A", "B"));
        Chunk<String> chunk2 = new Chunk<>(Arrays.asList("C", "D"));

        writer.write(chunk1);
        writer.write(chunk2);

        assertEquals(4, writer.getWrittenItems().size());
    }

    /**
     * Test implementation of AbstractDynamicWriter.
     */
    static class TestDynamicWriter extends AbstractDynamicWriter<String> {

        private final List<String> writtenItems = new ArrayList<>();
        private boolean preWriteCalled = false;
        private boolean postWriteCalled = false;
        private boolean errorHandled = false;
        private boolean throwError = false;

        @Override
        protected void writeItems(List<? extends String> items) throws Exception {
            if (throwError) {
                throw new RuntimeException("Write failed");
            }
            writtenItems.addAll(items);
        }

        @Override
        protected void preWrite(List<? extends String> items) {
            preWriteCalled = true;
        }

        @Override
        protected void postWrite(List<? extends String> items) {
            postWriteCalled = true;
        }

        @Override
        protected void handleWriteError(List<? extends String> items, Exception e) {
            errorHandled = true;
        }

        // Test helper methods
        List<String> getWrittenItems() {
            return writtenItems;
        }

        boolean isPreWriteCalled() {
            return preWriteCalled;
        }

        boolean isPostWriteCalled() {
            return postWriteCalled;
        }

        boolean isErrorHandled() {
            return errorHandled;
        }

        void setThrowError(boolean throwError) {
            this.throwError = throwError;
        }
    }
}
