package dev.gmky.utils.batch.core.writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * Abstract base class for item writers with pre/post processing hooks.
 * <p>
 * Provides template methods for write lifecycle management.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of items to write
 */
public abstract class AbstractDynamicWriter<T> implements ItemWriter<T>, ItemStream {

    @Override
    public void open(ExecutionContext executionContext) {
        // Default no-op implementation
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // Default no-op implementation
    }

    @Override
    public void close() {
        // Default no-op implementation
    }

    @Override
    public void write(Chunk<? extends T> chunk) throws Exception {
        List<? extends T> items = chunk.getItems();

        if (items.isEmpty()) {
            return;
        }

        // Pre-write hook
        preWrite(items);

        try {
            // Core write operation
            writeItems(items);

            // Post-write hook
            postWrite(items);
        } catch (Exception e) {
            handleWriteError(items, e);
            throw e;
        }
    }

    /**
     * Write the items - MUST be implemented by subclasses.
     *
     * @param items the items to write
     * @throws Exception if writing fails
     */
    protected abstract void writeItems(List<? extends T> items) throws Exception;

    /**
     * Pre-write hook called before writing items.
     * Override to perform preparation operations.
     *
     * @param items the items about to be written
     */
    protected void preWrite(List<? extends T> items) {
        // Hook for preparation
    }

    /**
     * Post-write hook called after successfully writing items.
     * Override to perform cleanup or logging.
     *
     * @param items the items that were written
     */
    protected void postWrite(List<? extends T> items) {
        // Hook for cleanup or logging
    }

    /**
     * Handle write errors.
     * Override to perform custom error handling.
     *
     * @param items the items that failed to write
     * @param e the exception that occurred
     */
    protected void handleWriteError(List<? extends T> items, Exception e) {
        // Override for custom error handling
    }
}
