package dev.gmky.utils.batch.core.processor;

import jakarta.annotation.Nonnull;
import org.springframework.batch.item.ItemProcessor;

/**
 * Abstract base class for item processors with pre/post processing hooks.
 * <p>
 * Provides template methods for validation, transformation, and filtering.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <I> the input type
 * @param <O> the output type
 */
public abstract class AbstractDynamicProcessor<I, O> implements ItemProcessor<I, O> {

    @Override
    public O process(@Nonnull I item) throws Exception {
        // Pre-processing hook
        if (!preProcess(item)) {
            return null; // Skip item
        }

        // Validation
        if (!validate(item)) {
            handleValidationFailure(item);
            return null;
        }

        // Core transformation
        O result = transform(item);

        // Post-processing hook
        return postProcess(result);
    }

    /**
     * Transform the input item to output.
     * This is the core processing logic that must be implemented.
     *
     * @param item the input item
     * @return the transformed output item
     * @throws Exception if transformation fails
     */
    protected abstract O transform(I item) throws Exception;

    /**
     * Pre-processing hook called before validation and transformation.
     * Return false to skip this item.
     *
     * @param item the input item
     * @return true to continue processing, false to skip
     */
    protected boolean preProcess(I item) {
        return true;
    }

    /**
     * Validate the input item.
     * Return false to skip this item and trigger validation failure handling.
     *
     * @param item the input item
     * @return true if valid, false if invalid
     */
    protected boolean validate(I item) {
        return true;
    }

    /**
     * Post-processing hook called after transformation.
     * Can be used to enrich or modify the output.
     *
     * @param item the output item
     * @return the processed output item
     */
    protected O postProcess(O item) {
        return item;
    }

    /**
     * Handle validation failures.
     * Override to log errors, collect failed items, etc.
     *
     * @param item the item that failed validation
     */
    protected void handleValidationFailure(I item) {
        // Override to handle validation failures
    }
}
