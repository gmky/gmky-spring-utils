package dev.gmky.utils.batch.core.reader;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for JPA paging readers.
 * <p>
 * Implements pagination to avoid loading all data into memory.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of items to read
 */
public abstract class AbstractJpaPagingReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

    protected int pageSize = 100;
    protected int page = 0;
    private List<T> results;
    private int currentIndex = 0;

    protected AbstractJpaPagingReader() {
        setName(ClassUtils.getShortName(getClass()));
    }

    @Override
    protected void doOpen() throws Exception {
        results = new ArrayList<>();
        page = 0;
        currentIndex = 0;
        onOpen();
    }

    @Override
    protected T doRead() throws Exception {
        if (results == null || currentIndex >= results.size()) {
            results = fetchPage(page++);
            currentIndex = 0;

            if (results.isEmpty()) {
                return null;
            }
        }

        T item = results.get(currentIndex++);
        return afterRead(item);
    }

    /**
     * Fetch a page of data - MUST be implemented by subclasses.
     *
     * @param pageNumber the page number to fetch (0-based)
     * @return list of items for this page
     * @throws Exception if an error occurs during fetching
     */
    protected abstract List<T> fetchPage(int pageNumber) throws Exception;

    /**
     * Hook called after opening the reader.
     * Override to perform custom initialization.
     *
     * @throws Exception if an error occurs during initialization
     */
    protected void onOpen() throws Exception {
        // Override if needed
    }

    /**
     * Hook called after reading each item.
     * Override to transform or detach entities.
     *
     * @param item the item that was read
     * @return the item (potentially transformed)
     */
    protected T afterRead(T item) {
        return item;
    }

    @Override
    protected void doClose() throws Exception {
        results = null;
        onClose();
    }

    /**
     * Hook called when closing the reader.
     * Override to perform custom cleanup.
     *
     * @throws Exception if an error occurs during cleanup
     */
    protected void onClose() throws Exception {
        // Override if needed
    }

    /**
     * Set the page size for pagination.
     *
     * @param pageSize the number of items per page
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get the current page size.
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }
}
