package dev.gmky.utils.batch.core.processor;

import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Composite processor that chains multiple processors together.
 * <p>
 * Items flow through processors sequentially.
 * If any processor returns null, the chain stops and null is returned.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <I> the input type
 * @param <O> the output type
 */
public class CompositeProcessor<I, O> implements ItemProcessor<I, O> {

    private final List<ItemProcessor<?, ?>> processors;

    /**
     * Constructor with varargs processors.
     *
     * @param processors the processors to chain
     */
    public CompositeProcessor(ItemProcessor<?, ?>... processors) {
        this.processors = Arrays.asList(processors);
    }

    /**
     * Constructor with list of processors.
     *
     * @param processors the processors to chain
     */
    public CompositeProcessor(List<ItemProcessor<?, ?>> processors) {
        this.processors = new ArrayList<>(processors);
    }

    @Override
    @SuppressWarnings("unchecked")
    public O process(I item) throws Exception {
        Object current = item;
        for (ItemProcessor processor : processors) {
            current = processor.process(current);
            if (current == null) {
                return null; // Stop chain if any processor returns null
            }
        }
        return (O) current;
    }

    /**
     * Get the list of processors in this composite.
     *
     * @return the list of processors
     */
    public List<ItemProcessor<?, ?>> getProcessors() {
        return new ArrayList<>(processors);
    }
}
