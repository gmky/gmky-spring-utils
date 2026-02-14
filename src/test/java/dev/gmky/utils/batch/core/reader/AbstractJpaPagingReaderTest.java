package dev.gmky.utils.batch.core.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbstractJpaPagingReader.
 */
class AbstractJpaPagingReaderTest {

    private TestJpaPagingReader reader;

    @BeforeEach
    void setUp() {
        reader = new TestJpaPagingReader();
    }

    @Test
    void testReadMultiplePages() throws Exception {
        // Setup test data: 25 items, page size 10 = 3 pages
        reader.setTotalItems(25);
        reader.setPageSize(10);
        reader.open(new ExecutionContext());

        // Read all items
        int count = 0;
        while (reader.read() != null) {
            count++;
        }

        assertEquals(25, count);
        // 3 pages of data (10, 10, 5) + 1 empty page to detect end = 4 pages
        assertEquals(4, reader.getPagesFetched());
    }

    @Test
    void testReadSinglePage() throws Exception {
        reader.setTotalItems(5);
        reader.setPageSize(10);
        reader.open(new ExecutionContext());

        int count = 0;
        while (reader.read() != null) {
            count++;
        }

        assertEquals(5, count);
        // 1 page of data + 1 empty page to detect end = 2 pages
        assertEquals(2, reader.getPagesFetched());
    }

    @Test
    void testReadEmptyDataset() throws Exception {
        reader.setTotalItems(0);
        reader.setPageSize(10);
        reader.open(new ExecutionContext());

        assertNull(reader.read());
        assertEquals(1, reader.getPagesFetched()); // Still fetches first page
    }

    @Test
    void testAfterReadHook() throws Exception {
        reader.setTotalItems(5);
        reader.setPageSize(10);
        reader.setTransformItems(true);
        reader.open(new ExecutionContext());

        String firstItem = reader.read();
        assertEquals("TRANSFORMED: Item 0", firstItem);
    }

    @Test
    void testPageSizeConfiguration() {
        reader.setPageSize(50);
        assertEquals(50, reader.getPageSize());
    }

    @Test
    void testOnOpenHook() {
        reader.setTotalItems(5);
        reader.open(new ExecutionContext());

        assertTrue(reader.isOpenCalled());
    }

    @Test
    void testOnCloseHook() {
        reader.setTotalItems(5);
        reader.open(new ExecutionContext());
        reader.close();

        assertTrue(reader.isCloseCalled());
    }

    /**
     * Test implementation of AbstractJpaPagingReader for testing purposes.
     */
    static class TestJpaPagingReader extends AbstractJpaPagingReader<String> {

        private int totalItems = 0;
        private int pagesFetched = 0;
        private boolean openCalled = false;
        private boolean closeCalled = false;
        private boolean transformItems = false;

        @Override
        protected List<String> fetchPage(int pageNumber) {
            pagesFetched++;
            int startIndex = pageNumber * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalItems);

            if (startIndex >= totalItems) {
                return Collections.emptyList();
            }

            String[] items = new String[endIndex - startIndex];
            for (int i = 0; i < items.length; i++) {
                items[i] = "Item " + (startIndex + i);
            }
            return Arrays.asList(items);
        }

        @Override
        protected void onOpen() {
            openCalled = true;
        }

        @Override
        protected String afterRead(String item) {
            if (transformItems) {
                return "TRANSFORMED: " + item;
            }
            return item;
        }

        @Override
        protected void onClose() {
            closeCalled = true;
        }

        // Test helper methods
        void setTotalItems(int totalItems) {
            this.totalItems = totalItems;
        }

        int getPagesFetched() {
            return pagesFetched;
        }

        boolean isOpenCalled() {
            return openCalled;
        }

        boolean isCloseCalled() {
            return closeCalled;
        }

        void setTransformItems(boolean transformItems) {
            this.transformItems = transformItems;
        }
    }
}
