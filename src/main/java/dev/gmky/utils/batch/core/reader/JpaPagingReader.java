package dev.gmky.utils.batch.core.reader;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JPA reader with pagination support using JPQL queries.
 * <p>
 * Fetches data in pages to avoid memory issues with large datasets.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 * @param <T> the type of entity to read
 */
public class JpaPagingReader<T> extends AbstractJpaPagingReader<T> {

    private final EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private final String jpqlQuery;
    private final Map<String, Object> parameters;
    private final Class<T> entityClass;

    /**
     * Constructor with all parameters.
     *
     * @param entityManagerFactory the entity manager factory
     * @param jpqlQuery the JPQL query to execute
     * @param entityClass the entity class
     * @param pageSize the page size
     * @param parameters query parameters
     */
    public JpaPagingReader(EntityManagerFactory entityManagerFactory,
                          String jpqlQuery,
                          Class<T> entityClass,
                          int pageSize,
                          Map<String, Object> parameters) {
        this.entityManagerFactory = entityManagerFactory;
        this.jpqlQuery = jpqlQuery;
        this.entityClass = entityClass;
        this.pageSize = pageSize;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        setName(ClassUtils.getShortName(getClass()));
    }

    /**
     * Constructor without parameters.
     */
    public JpaPagingReader(EntityManagerFactory entityManagerFactory,
                          String jpqlQuery,
                          Class<T> entityClass,
                          int pageSize) {
        this(entityManagerFactory, jpqlQuery, entityClass, pageSize, null);
    }

    @Override
    protected void onOpen() {
        entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    protected List<T> fetchPage(int pageNumber) {
        TypedQuery<T> query = entityManager.createQuery(jpqlQuery, entityClass);

        // Set parameters
        parameters.forEach(query::setParameter);

        // Pagination
        query.setFirstResult(pageNumber * pageSize);
        query.setMaxResults(pageSize);

        List<T> pageResults = query.getResultList();

        // Clear persistence context to avoid memory issues
        entityManager.clear();

        return pageResults;
    }

    @Override
    protected T afterRead(T item) {
        // Item is already detached after clear()
        return item;
    }

    @Override
    protected void onClose() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }
}
