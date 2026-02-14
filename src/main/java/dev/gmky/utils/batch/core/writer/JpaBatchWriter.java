package dev.gmky.utils.batch.core.writer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;

/**
 * JPA batch writer that persists entities in batches.
 * <p>
 * Uses EntityManager for batch inserts/updates with periodic flushing.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of entity to write
 */
public class JpaBatchWriter<T> extends AbstractDynamicWriter<T> {

    private final EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private final int batchSize;
    private final boolean merge;

    /**
     * Constructor with all parameters.
     *
     * @param entityManagerFactory the entity manager factory
     * @param batchSize the batch size for flushing
     * @param merge if true, use merge instead of persist
     */
    public JpaBatchWriter(EntityManagerFactory entityManagerFactory, int batchSize, boolean merge) {
        this.entityManagerFactory = entityManagerFactory;
        this.batchSize = batchSize;
        this.merge = merge;
    }

    /**
     * Constructor with persist mode (default).
     *
     * @param entityManagerFactory the entity manager factory
     * @param batchSize the batch size for flushing
     */
    public JpaBatchWriter(EntityManagerFactory entityManagerFactory, int batchSize) {
        this(entityManagerFactory, batchSize, false);
    }

    /**
     * Constructor with default batch size (50).
     *
     * @param entityManagerFactory the entity manager factory
     */
    public JpaBatchWriter(EntityManagerFactory entityManagerFactory) {
        this(entityManagerFactory, 50, false);
    }

    @Override
    public void open(org.springframework.batch.item.ExecutionContext executionContext) {
        entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    protected void writeItems(List<? extends T> items) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        try {
            for (int i = 0; i < items.size(); i++) {
                T item = items.get(i);

                if (merge) {
                    entityManager.merge(item);
                } else {
                    entityManager.persist(item);
                }

                // Flush and clear periodically to avoid memory issues
                if (i > 0 && i % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }

            // Final flush and clear
            entityManager.flush();
            entityManager.clear();

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }
}
